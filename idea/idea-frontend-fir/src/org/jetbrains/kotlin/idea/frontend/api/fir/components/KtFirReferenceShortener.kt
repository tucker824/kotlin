/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import com.intellij.psi.util.parentsOfType
import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.builder.buildResolvedImport
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.core.thisOrParentIsRoot
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacadeForResolveOnAir
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.idea.frontend.api.components.KtReferenceShortener
import org.jetbrains.kotlin.idea.frontend.api.components.ShortenCommand
import org.jetbrains.kotlin.idea.frontend.api.components.ShortenOption
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.addImportToFile
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability

internal class KtFirReferenceShortener(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
    override val firResolveState: FirModuleResolveState,
) : KtReferenceShortener(), KtFirAnalysisSessionComponent {
    private val context = FirShorteningContext(firResolveState)

    override fun collectShortenings(
        file: KtFile,
        selection: TextRange,
        classShortenOption: (KtClassOrObjectSymbol) -> ShortenOption,
        callableShortenOption: (KtCallableSymbol) -> ShortenOption
    ): ShortenCommand {
        val declarationToVisit = file.findSmallestDeclarationContainingSelection(selection)
            ?: file.withDeclarationsResolvedToBodyResolve()

        val firDeclaration = declarationToVisit.getOrBuildFir(firResolveState)

        val towerContext =
            LowLevelFirApiFacadeForResolveOnAir.onAirGetTowerContextProvider(firResolveState, declarationToVisit)

        //TODO: collect all usages of available symbols in the file and prevent importing symbols that could introduce name clashes, which
        // may alter the meaning of existing code.
        val collector = ElementsToShortenCollector(
            context,
            towerContext,
            classShortenOption = { classShortenOption(analysisSession.firSymbolBuilder.buildSymbol(it.fir) as KtClassOrObjectSymbol) },
            callableShortenOption = { callableShortenOption(analysisSession.firSymbolBuilder.buildSymbol(it.fir) as KtCallableSymbol) })
        firDeclaration.accept(collector)

        return ShortenCommandImpl(
            file,
            collector.namesToImport.distinct(),
            collector.namesToImportWithStar.distinct(),
            collector.typesToShorten.distinct().map { it.createSmartPointer() },
            collector.qualifiersToShorten.distinct().map { it.createSmartPointer() }
        )
    }

    private fun KtFile.withDeclarationsResolvedToBodyResolve(): KtFile {
        for (declaration in declarations) {
            declaration.getOrBuildFir(firResolveState) // temporary hack, resolves declaration to BODY_RESOLVE stage
        }

        return this
    }
}

private fun KtFile.findSmallestDeclarationContainingSelection(selection: TextRange): KtDeclaration? =
    findElementAt(selection.startOffset)
        ?.parentsOfType<KtDeclaration>(withSelf = true)
        ?.firstOrNull { selection in it.textRange }

private data class AvailableSymbol<out T>(
    val symbol: T,
    val isFromStarImport: Boolean,
    val isFromPackageImport: Boolean,
    val isFromDefaultImport: Boolean,
)

private class FirShorteningContext(val firResolveState: FirModuleResolveState) {

    val firSession: FirSession
        get() = firResolveState.rootModuleSession

    fun findFirstClassifierInScopesByName(positionScopes: List<FirScope>, targetClassName: Name): AvailableSymbol<ClassId>? {
        for (scope in positionScopes) {
            val classifierSymbol = scope.findFirstClassifierByName(targetClassName) ?: continue
            val classifierLookupTag = classifierSymbol.toLookupTag() as? ConeClassLikeLookupTag ?: continue

            return AvailableSymbol(
                classifierLookupTag.classId,
                isFromStarImport = scope is FirAbstractStarImportingScope,
                isFromPackageImport = scope is FirPackageMemberScope,
                isFromDefaultImport = scope is FirDefaultStarImportingScope || scope is FirDefaultSimpleImportingScope,
            )
        }

        return null
    }

    fun findFunctionsInScopes(scopes: List<FirScope>, name: Name): List<AvailableSymbol<FirNamedFunctionSymbol>> {
        return scopes.flatMap { scope ->
            scope.getFunctions(name).map {
                AvailableSymbol(
                    it,
                    scope is FirAbstractStarImportingScope,
                    scope is FirPackageMemberScope,
                    scope is FirDefaultStarImportingScope || scope is FirDefaultSimpleImportingScope
                )
            }
        }
    }

    fun findPropertiesInScopes(scopes: List<FirScope>, name: Name): List<AvailableSymbol<FirVariableSymbol<*>>> {
        return scopes.flatMap { scope ->
            scope.getProperties(name).map {
                AvailableSymbol(
                    it,
                    scope is FirAbstractStarImportingScope,
                    scope is FirPackageMemberScope,
                    scope is FirDefaultStarImportingScope || scope is FirDefaultSimpleImportingScope
                )
            }
        }
    }

    private fun FirScope.findFirstClassifierByName(name: Name): FirClassifierSymbol<*>? {
        var element: FirClassifierSymbol<*>? = null

        processClassifiersByName(name) {
            if (element == null) {
                element = it
            }
        }

        return element
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun findScopesAtPosition(
        position: KtElement,
        newImports: List<FqName>,
        towerContextProvider: FirTowerContextProvider
    ): List<FirScope>? {
        val towerDataContext = towerContextProvider.getClosestAvailableParentContext(position) ?: return null
        val result = buildList<FirScope> {
            addAll(towerDataContext.nonLocalTowerDataElements.mapNotNull { it.scope })
            addIfNotNull(createFakeImportingScope(newImports))
            addAll(towerDataContext.localScopes)
        }

        return result.asReversed()
    }

    private fun createFakeImportingScope(newImports: List<FqName>): FirScope? {
        val resolvedNewImports = newImports.mapNotNull { createFakeResolvedImport(it) }
        if (resolvedNewImports.isEmpty()) return null

        return FirExplicitSimpleImportingScope(resolvedNewImports, firSession, ScopeSession())
    }

    private fun createFakeResolvedImport(fqNameToImport: FqName): FirResolvedImport? {
        val packageOrClass = resolveToPackageOrClass(firSession.symbolProvider, fqNameToImport) ?: return null

        val delegateImport = buildImport {
            importedFqName = fqNameToImport
            isAllUnder = false
        }

        return buildResolvedImport {
            delegate = delegateImport
            packageFqName = packageOrClass.packageFqName
        }
    }

    fun getRegularClass(typeRef: FirTypeRef): FirRegularClass? {
        return typeRef.toRegularClass(firSession)
    }
}

private sealed class ElementToShorten {
    abstract val nameToImport: FqName?
    abstract val importAllInParent: Boolean
}

private class ShortenType(
    val element: KtUserType,
    override val nameToImport: FqName? = null,
    override val importAllInParent: Boolean = false
) : ElementToShorten()

private class ShortenQualifier(
    val element: KtDotQualifiedExpression,
    override val nameToImport: FqName? = null,
    override val importAllInParent: Boolean = false
) : ElementToShorten()

private class ElementsToShortenCollector(
    private val shorteningContext: FirShorteningContext,
    private val towerContextProvider: FirTowerContextProvider,
    private val classShortenOption: (FirRegularClassSymbol) -> ShortenOption,
    private val callableShortenOption: (FirCallableSymbol<*>) -> ShortenOption,
) :
    FirVisitorVoid() {
    val namesToImport: MutableList<FqName> = mutableListOf()
    val namesToImportWithStar: MutableList<FqName> = mutableListOf()
    val typesToShorten: MutableList<KtUserType> = mutableListOf()
    val qualifiersToShorten: MutableList<KtDotQualifiedExpression> = mutableListOf()

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        processTypeRef(resolvedTypeRef)

        resolvedTypeRef.acceptChildren(this)
        resolvedTypeRef.delegatedTypeRef?.accept(this)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
        super.visitResolvedQualifier(resolvedQualifier)

        processTypeQualifier(resolvedQualifier)
    }

    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        super.visitResolvedNamedReference(resolvedNamedReference)

        processPropertyReference(resolvedNamedReference)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        super.visitFunctionCall(functionCall)

        processFunctionCall(functionCall)
    }

    private fun processTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        val wholeTypeReference = resolvedTypeRef.psi as? KtTypeReference ?: return

        val wholeClassifierId = resolvedTypeRef.type.lowerBoundIfFlexible().classId ?: return
        val wholeTypeElement = wholeTypeReference.typeElement?.unwrapNullability() as? KtUserType ?: return

        if (wholeTypeElement.qualifier == null) return

        findTypeToShorten(wholeClassifierId, wholeTypeElement)?.let(::addElementToShorten)
    }

    private fun findTypeToShorten(wholeClassifierId: ClassId, wholeTypeElement: KtUserType): ElementToShorten? {
        val positionScopes = shorteningContext.findScopesAtPosition(wholeTypeElement, namesToImport, towerContextProvider) ?: return null
        val allClassIds = wholeClassifierId.outerClassesWithSelf
        val allTypeElements = wholeTypeElement.qualifiersWithSelf
        return findElementsToShorten(positionScopes, allClassIds, allTypeElements, ::ShortenType, this::findFakePackageToShorten) {
            it.qualifier != null
        }
    }

    private fun findFakePackageToShorten(typeElement: KtUserType): ShortenType? {
        val deepestTypeWithQualifier = typeElement.qualifiersWithSelf.last().parent as? KtUserType
            ?: error("Type element should have at least one qualifier, instead it was ${typeElement.text}")

        return if (deepestTypeWithQualifier.hasFakeRootPrefix()) ShortenType(deepestTypeWithQualifier) else null
    }

    private fun processTypeQualifier(resolvedQualifier: FirResolvedQualifier) {
        val wholeClassQualifier = resolvedQualifier.classId ?: return
        val wholeQualifierElement = when (val qualifierPsi = resolvedQualifier.psi) {
            is KtDotQualifiedExpression -> qualifierPsi
            is KtNameReferenceExpression -> qualifierPsi.getDotQualifiedExpressionForSelector() ?: return
            else -> return
        }

        findTypeQualifierToShorten(wholeClassQualifier, wholeQualifierElement)?.let(::addElementToShorten)
    }

    private fun findTypeQualifierToShorten(
        wholeClassQualifier: ClassId,
        wholeQualifierElement: KtDotQualifiedExpression
    ): ElementToShorten? {
        val positionScopes: List<FirScope> =
            shorteningContext.findScopesAtPosition(wholeQualifierElement, namesToImport, towerContextProvider) ?: return null
        val allClassIds: Sequence<ClassId> = wholeClassQualifier.outerClassesWithSelf
        val allQualifiers: Sequence<KtDotQualifiedExpression> = wholeQualifierElement.qualifiersWithSelf
        return findElementsToShorten(positionScopes, allClassIds, allQualifiers, ::ShortenQualifier, this::findFakePackageToShorten)
    }

    private inline fun <E> findElementsToShorten(
        positionScopes: List<FirScope>,
        allClassIds: Sequence<ClassId>,
        allElements: Sequence<E>,
        createElementToShorten: (E, nameToImport: FqName?, importAllInParent: Boolean) -> ElementToShorten,
        findFakePackageToShortenFn: (E) -> ElementToShorten?,
        elementFilter: (E) -> Boolean = { true },
    ): ElementToShorten? {

        for ((classId, element) in allClassIds.zip(allElements)) {
            if (!elementFilter(element)) return null
            val option = classShortenOption(classId.toRegularClass() ?: return null)
            if (option == ShortenOption.DO_NOT_SHORTEN) continue

            // Find class with the same name that's already available in this file.
            val availableClassifier = shorteningContext.findFirstClassifierInScopesByName(positionScopes, classId.shortClassName)

            when {
                // No class with name `classId.shortClassName` is present in the scope. Hence, we can safely import the name and shorten
                // the reference.
                availableClassifier == null -> {
                    // Caller indicates don't shorten if doing that needs importing more names. Hence, we just skip.
                    if (option == ShortenOption.SHORTEN_IF_ALREADY_IMPORTED) continue
                    return createElementToShorten(
                        element,
                        classId.asSingleFqName(),
                        option == ShortenOption.SHORTEN_AND_IMPORT_ALL_IN_PARENT
                    )
                }
                // The class with name `classId.shortClassName` happens to be the same class referenced by this qualified access.
                availableClassifier.symbol == classId -> {
                    // Respect caller's request to use star import, if it's not already star-imported.
                    return when {
                        !availableClassifier.isFromPackageImport && !availableClassifier.isFromStarImport &&
                                option == ShortenOption.SHORTEN_AND_IMPORT_ALL_IN_PARENT -> {
                            createElementToShorten(element, classId.asSingleFqName(), true)
                        }
                        // Otherwise, just shorten it and don't alter import statements
                        else -> createElementToShorten(element, null, false)
                    }
                }
                // Allow using star import to overwrite members implicitly imported by default.
                availableClassifier.isFromDefaultImport && availableClassifier.isFromStarImport && option == ShortenOption.SHORTEN_AND_IMPORT_ALL_IN_PARENT -> {
                    return createElementToShorten(element, classId.asSingleFqName(), true)
                }
                // Allow using explicit import to overwrite members imported on-demand or in package
                (availableClassifier.isFromStarImport || availableClassifier.isFromPackageImport) && option == ShortenOption.SHORTEN_AND_IMPORT -> {
                    return createElementToShorten(element, classId.asSingleFqName(), false)
                }
            }
        }
        return findFakePackageToShortenFn(allElements.last())
    }

    private fun ClassId.toRegularClass() =
        shorteningContext.firSession.symbolProvider.getClassLikeSymbolByFqName(this) as? FirRegularClassSymbol

    private fun processPropertyReference(resolvedNamedReference: FirResolvedNamedReference) {
        val referenceExpression = resolvedNamedReference.psi as? KtNameReferenceExpression
        val qualifiedProperty = referenceExpression?.getDotQualifiedExpressionForSelector() ?: return

        val callableSymbol = resolvedNamedReference.resolvedSymbol as? FirCallableSymbol<*> ?: return
        processQualifiedAccess(callableSymbol, qualifiedProperty, qualifiedProperty, shorteningContext::findPropertiesInScopes)
    }

    private fun processFunctionCall(functionCall: FirFunctionCall) {
        if (!canBePossibleToDropReceiver(functionCall)) return

        val qualifiedCallExpression = functionCall.psi as? KtDotQualifiedExpression ?: return
        val callExpression = qualifiedCallExpression.selectorExpression as? KtCallExpression ?: return

        val calleeReference = functionCall.calleeReference
        val calledSymbol = findUnambiguousReferencedCallableId(calleeReference) ?: return
        processQualifiedAccess(calledSymbol, qualifiedCallExpression, callExpression, shorteningContext::findFunctionsInScopes)
    }

    private fun processQualifiedAccess(
        calledSymbol: FirCallableSymbol<*>,
        qualifiedCallExpression: KtDotQualifiedExpression,
        expressionToGetScope: KtExpression,
        findCallableInScopes: (List<FirScope>, Name) -> List<AvailableSymbol<FirCallableSymbol<*>>>,
    ) {
        val option = callableShortenOption(calledSymbol)
        if (option == ShortenOption.DO_NOT_SHORTEN) return
        val callableId = calledSymbol.callableId

        val scopes = shorteningContext.findScopesAtPosition(expressionToGetScope, namesToImport, towerContextProvider) ?: return
        val availableCallables = findCallableInScopes(scopes, callableId.callableName)

        val nameToImport = if (calledSymbol is FirConstructorSymbol) {
            // A constructor is imported by the class name.
            calledSymbol.containingClass()?.classId?.asSingleFqName()
        } else {
            callableId.asImportableFqName(shorteningContext.firSession)
        }
        val (matchedCallables, otherCallables) = availableCallables.partition { it.symbol.callableId == callableId }
        val callToShorten = when {
            otherCallables.all { it.isFromDefaultImport && it.isFromStarImport } -> {
                val isFromPackageImport = matchedCallables.any { it.isFromPackageImport }
                val isFromStarImport = matchedCallables.any { it.isFromStarImport }
                when {
                    matchedCallables.isEmpty() -> {
                        if (nameToImport == null || option == ShortenOption.SHORTEN_IF_ALREADY_IMPORTED) return
                        ShortenQualifier(
                            qualifiedCallExpression,
                            nameToImport,
                            option == ShortenOption.SHORTEN_AND_IMPORT_ALL_IN_PARENT
                        )
                    }
                    !isFromPackageImport && !isFromStarImport && option == ShortenOption.SHORTEN_AND_IMPORT_ALL_IN_PARENT ->
                        ShortenQualifier(qualifiedCallExpression, nameToImport, true)
                    else -> ShortenQualifier(qualifiedCallExpression)
                }
            }
            else -> findFakePackageToShorten(qualifiedCallExpression)
        }

        callToShorten?.let(::addElementToShorten)
    }

    private fun canBePossibleToDropReceiver(functionCall: FirFunctionCall): Boolean {
        // we can remove receiver only if it is a qualifier
        val explicitReceiver = functionCall.explicitReceiver as? FirResolvedQualifier ?: return false

        // if there is no extension receiver necessary, then it can be removed
        if (functionCall.extensionReceiver is FirNoReceiverExpression) return true

        val receiverType = shorteningContext.getRegularClass(explicitReceiver.typeRef) ?: return true
        return receiverType.classKind != ClassKind.OBJECT
    }

    private fun findUnambiguousReferencedCallableId(namedReference: FirNamedReference): FirCallableSymbol<*>? {
        val unambiguousSymbol = when (namedReference) {
            is FirResolvedNamedReference -> namedReference.resolvedSymbol
            is FirErrorNamedReference -> {
                val candidateSymbol = namedReference.candidateSymbol
                if (candidateSymbol !is FirErrorFunctionSymbol) {
                    candidateSymbol
                } else {
                    getSingleUnambiguousCandidate(namedReference)
                }
            }
            else -> null
        }

        return (unambiguousSymbol as? FirCallableSymbol<*>)
    }

    /**
     * If [namedReference] is ambiguous and all candidates point to the callables with same callableId,
     * returns the first candidate; otherwise returns null.
     */
    private fun getSingleUnambiguousCandidate(namedReference: FirErrorNamedReference): FirCallableSymbol<*>? {
        val coneAmbiguityError = namedReference.diagnostic as? ConeAmbiguityError ?: return null

        val candidates = coneAmbiguityError.candidates.map { it.symbol as FirCallableSymbol<*> }
        require(candidates.isNotEmpty()) { "Cannot have zero candidates" }

        val distinctCandidates = candidates.distinctBy { it.callableId }
        return distinctCandidates.singleOrNull()
            ?: error("Expected all candidates to have same callableId, but got: ${distinctCandidates.map { it.callableId }}")
    }

    private fun findFakePackageToShorten(wholeQualifiedExpression: KtDotQualifiedExpression): ShortenQualifier? {
        val deepestQualifier = wholeQualifiedExpression.qualifiersWithSelf.last()
        return if (deepestQualifier.hasFakeRootPrefix()) ShortenQualifier(deepestQualifier) else null
    }

    private fun addElementToShorten(element: ElementToShorten) {
        if (element.importAllInParent && element.nameToImport?.thisOrParentIsRoot() == false) {
            namesToImportWithStar.addIfNotNull(element.nameToImport?.parent())
        } else {
            namesToImport.addIfNotNull(element.nameToImport)
        }
        when (element) {
            is ShortenType -> {
                typesToShorten.add(element.element)
            }
            is ShortenQualifier -> {
                qualifiersToShorten.add(element.element)
            }
        }
    }

    private val ClassId.outerClassesWithSelf: Sequence<ClassId>
        get() = generateSequence(this) { it.outerClassId }

    private val KtUserType.qualifiersWithSelf: Sequence<KtUserType>
        get() = generateSequence(this) { it.qualifier }

    private val KtDotQualifiedExpression.qualifiersWithSelf: Sequence<KtDotQualifiedExpression>
        get() = generateSequence(this) { it.receiverExpression as? KtDotQualifiedExpression }
}

private class ShortenCommandImpl(
    val targetFile: KtFile,
    val importsToAdd: List<FqName>,
    val starImportsToAdd: List<FqName>,
    val typesToShorten: List<SmartPsiElementPointer<KtUserType>>,
    val qualifiersToShorten: List<SmartPsiElementPointer<KtDotQualifiedExpression>>,
) : ShortenCommand {

    override fun invokeShortening() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        for (nameToImport in importsToAdd) {
            addImportToFile(targetFile.project, targetFile, nameToImport)
        }

        for (nameToImport in starImportsToAdd) {
            addImportToFile(targetFile.project, targetFile, nameToImport, allUnder = true)
        }

        PostprocessReformattingAspect.getInstance(targetFile.project).disablePostprocessFormattingInside {
            for (typePointer in typesToShorten) {
                val type = typePointer.element ?: continue
                type.deleteQualifier()
            }

            for (callPointer in qualifiersToShorten) {
                val call = callPointer.element ?: continue
                call.deleteQualifier()
            }
        }
    }

    override val isEmpty: Boolean get() = typesToShorten.isEmpty() && qualifiersToShorten.isEmpty()
}

private fun KtUserType.hasFakeRootPrefix(): Boolean =
    qualifier?.referencedName == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

private fun KtDotQualifiedExpression.hasFakeRootPrefix(): Boolean =
    (receiverExpression as? KtNameReferenceExpression)?.getReferencedName() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

private fun CallableId.asImportableFqName(firSession: FirSession): FqName? {
    val classId = classId
    return if (classId == null) {
        packageName.child(callableName)
    } else {
        // Java static members, enums, and object members can be imported
        val containingClass = firSession.symbolProvider.getClassLikeSymbolByFqName(classId)?.fir as? FirRegularClass ?: return null
        if (containingClass.origin == FirDeclarationOrigin.Java ||
            containingClass.classKind == ClassKind.ENUM_CLASS ||
            containingClass.classKind == ClassKind.OBJECT
        ) {
            asSingleFqName()
        } else {
            null
        }
    }
}

private fun KtElement.getDotQualifiedExpressionForSelector(): KtDotQualifiedExpression? =
    getQualifiedExpressionForSelector() as? KtDotQualifiedExpression

private fun KtDotQualifiedExpression.deleteQualifier(): KtExpression? {
    val selectorExpression = selectorExpression ?: return null
    return this.replace(selectorExpression) as KtExpression
}

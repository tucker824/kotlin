#!/bin/bash

git cherry-pick -x 561cde9d069dd565d0f9614afd6a3f57b5cc2322 &&
  git cherry-pick -x f51a0048f666d0783236ae22be140a66eea30244 &&
  git cherry-pick -x 7f51be9cd340a3cea9d51ad863b43d9fe3cf75fd &&
  git cherry-pick -x 82f48cdd11e042106edaee93beb7bf511be11782 &&
  git cherry-pick -x 54d707b3b631def1e03afe92c9434549f05d81ab &&
  # Already in 1.3.70.
  # git cherry-pick -x 776736a25abe8ded3b144f22d64477d428c98c9a &&
  # git cherry-pick -x b6de3c2fcc92ee0647251ae9ffea32df5ed51cc7 &&
  git cherry-pick -x e92985458bf94af5589a9380e06196335d55a0ac &&
  git cherry-pick -x 220ea72d65e67c5be713ee62f1ebef0b65575ab3 &&
  git cherry-pick -x 0d8036bb140c3353775180eb8352e51617dce4f8 &&
  git cherry-pick -x 5d3ef4c632d503dd3e20314f586e863ebd53d33e &&
  git cherry-pick -x 0cb48999ff67a3a689893c0d70286b20d98e58b8 &&
  git cherry-pick -x 09c3279cc76d338a46de27a122a59fb98447d4bd &&
  git cherry-pick -x a4b005fd5d2a52f31bfd76528d4c74d8986f54e4 &&

  # Potentially more here with fir tests and infrastructure
  # for muting:
  #git cherry-pick -x 2adcb5dec474d532fbd6387bad4b6a8df7116499 &&
  #git cherry-pick -x 26822a0cdeabab4e8d14cf1cb18fe62f0f64b02c &&

  git cherry-pick -x fefdce04066f8c1ab2980182bd720de2fe1d1bd2 &&
  git cherry-pick -x 17d2fda9465e6974b19a4cd77207b0e570de533c &&
  git cherry-pick -x 59f2aa7addcb8083bef08cc16b9149772c1514c5 &&
  git cherry-pick -x e261b1e2de5f67ec097cf1df0c1814287d638d30 &&
  git cherry-pick -x 51f726be9bd6830110163b791043d7345638e7f3 &&
  git cherry-pick -x 3b37f6bd32c5afb444ca9e563051c2c018ba5c87 &&
  git cherry-pick -x a1448ebb371e18532732980333fd51a1a945f0af &&
  git cherry-pick -x daa76cbf1e43fbe0b8fd37e8fe249e08cd1c1295 &&
  git cherry-pick -x 9292022f880c6313ec856f0a35818468717ce885 &&
  git cherry-pick -x 5c92da3f35bc3c7261bcda372244f2fb5a3da937 &&

  # Optimize imports. Silly, but makes merging easier
  git cherry-pick -x 0667ee97962f37dc00d985ccaebdeee1e7f924cd &&

  git cherry-pick -x 1ea89ce28edc8818363adc6125863554ca960964 &&
  git cherry-pick -x 068d3f4beb5af0b4885769221a543f8c77a8aad1 &&
  git cherry-pick -x d4b0151f51b1b4611a38bec31bddb94cfcf991b1 &&
  git cherry-pick -x cdf9ef63ba5e6568574a5d6e26a75e589a385e8a &&
  git cherry-pick -x 982a088f002f2a90f870ae13515593c0be0642c2 &&

  # Fold constant lowering refactorings.
  git cherry-pick -x 738db7e511bf252531ed5a6eb18bff22b5c65b92 &&
  git cherry-pick -x --strategy-option=ours 6ba8fbd45195ef041d70cb2aba6961e3fda8e726 &&
  
  git cherry-pick -x a8e9a6a1d07f920a5f093ededd3a514bc2a2ed6f &&
  git cherry-pick -x e54ef3bdb84a1bbc3f2185e6c1cd1a12794f1705 &&
  git cherry-pick -x 2dd8727baf0927faa60db38427005eb7171780ba &&
  git cherry-pick -x 1d5370a56a1a05361afb4277f542fc7eddb69599 &&
  git cherry-pick -x babe6eb581fc995c2de24c243953ce34d5ba6b25 &&
  git cherry-pick -x a18fecb05f3c7593c315b6593540ea509f263263 &&
  git cherry-pick -x 4b6202c9024d8afba2fecf9c5649e15a2fe23217 &&
  git cherry-pick -x 98bf0e278f2a7b9d0b2a8ec962493dff8e2893ff &&
  git cherry-pick -x d62254282409bda6fc870bdef35f5df6d9ba7b18 &&
  git cherry-pick -x 0e4e5ac287c3836ca4434c2c6f626b9145c284e5 &&
  git cherry-pick -x 8054e2960e354a598f864672d3d0fc8af70b3ed3 &&
  git cherry-pick -x d27593aedaafa8f9661a9f6d8e895dacaf69c2ee &&
  git cherry-pick -x 2ebb797e61632d35f70413c8a0b75b7e218203b4 &&
  git cherry-pick -x e7835fecfc93349e9bc7e79adcbb90b92597279b &&
  git cherry-pick -x 98f5c5aa95a505cfd9c98ac7e2b96d5163ea3041 &&
  git cherry-pick -x 6b5d92a6935146391b887319ab752e91febbd4dc &&
  git cherry-pick -x 7f319c18de4dd4397f97d7a4c461daf4c4288cf4 &&
  git cherry-pick -x 1b1dff9191a46481e6daa0781c9749d5d075a2de &&
  git cherry-pick -x bd74e976c99481f4413f4e10914573bea4ffaffe &&
  git cherry-pick -x 5ffbf9264a6452f4fe0bffbd271c64af2f53ce09 &&
  git cherry-pick -x 5309e774ac7659ae3d4af8162604f2491bbc59bb &&
  git cherry-pick -x 70b304e6e4d54021ff8e44577feb89c0ac0e66ce &&
  git cherry-pick -x 36c4df6d99029a63bb690cf2bc402b37ebeb8415 &&
  git cherry-pick -x 137ef26723739b41c4c7f5b58002fc46553e14f2 &&
  git cherry-pick -x ef5fe0675a7cd37ea35ec80b0ccd1e5ddb0f9d5d &&
  git cherry-pick -x 88cac53d88b648be41deedaf9872ce72e9915122 &&
  git cherry-pick -x 31ba2d64db8edc2d56a5eebfbfcf2164489bd21f &&
  git cherry-pick -x 4e4e57f60a728c36f989b714ffa964e922429118 &&
  git cherry-pick -x a67df82b1e3fc876e6eff21aaada06c57c89ded2 &&
  git cherry-pick -x d81231fdf70f4da4a79588223b3244704dc5db7e &&
  git cherry-pick -x 01da7f289b077cf77a2638f60d32a20f40c02c1d &&
  git cherry-pick -x 8d0ffa14442a57d893bbd37465df36d2dfdc7b94 &&
  git cherry-pick -x c47e04ac8d3b652a2f3580b07d11ea1519d0700e &&
  git cherry-pick -x 957b100cd1f404c048c5b16163fc61a74689d688 &&
  git cherry-pick -x d6ed93b2b873d6af1e08c345343d916be3df3f01 &&
  git cherry-pick -x 3193689086e03f892b7dd95e8aa12b8ba0d0ac4f &&
  git cherry-pick -x 3848ac9cac6a2d27af727e63054f4b77b1b855ae &&
  git cherry-pick -x b48d7f4ba727266bcf4e718c83672047aa9589d2 &&
  git cherry-pick -x c948459ed53d6d82eae884ed86f90e43b171f400 &&
  git cherry-pick -x b2f8a4e82a964a68ed55999a5ce63e4c1a3e256d &&
  git cherry-pick -x 2b4dc1199aba87e2b028df2b5577c46cc14432e7 &&
  git cherry-pick -x a5ff88f89735d2b62c0422183293092a4171482c &&
  git cherry-pick -x 0abdd0cb7b0bd0f19569437e877539209368197a &&
  git cherry-pick -x 049bb54ea6140de83c8ab8d89c1c2b68f0ed01f3 &&
  git cherry-pick -x be228d594c7d4d5962a5fc20c0bcf0fb20326c76 &&
  git cherry-pick -x 04a6f4d92bdde310e933dd34249296ec89e17a87 &&
  git cherry-pick -x 6ad159bb0198442ab2ab5ceb94a5dd47289d6a81 &&
  git cherry-pick -x a16b21a7cc37d0f8201471e030b595f520747da7 &&
  git cherry-pick -x 99eab5a0582b37754478297bf7f7cde4978d6706 &&
  git cherry-pick -x f73891af9802720c6ec429b83e18241d346950b7 &&
  git cherry-pick -x e46adbae29dd919546862fd8267a47b138388794 &&
  git cherry-pick -x ca05ff1791064f842a8dbf3098e4276f4e0dd6c0 &&

  # Renaming of experimental / use experimental here if needed.

  git cherry-pick -x 83edc7fa7310357a71cf58864e67b35eb504fb3a &&
  git cherry-pick -x f8779ddf9dc92c9f4fd7b60deae05a71eeb775a0 &&
  git cherry-pick -x b0e61ab470d895fb44ccbd8be323d390bd693d80 &&
  git cherry-pick -x 1b5109b6eeefe7105ddf2a65b8949dcf97205346 &&
  git cherry-pick -x cb98588202a946cd66ec4ea215d9ee4b3ce8231f &&
  git cherry-pick -x 02722e02386e005f304cc44b26a2406093ba273c &&

  # Proper visibility for companion object...
  # 3d85e5da5ff96a0b10f9b24d6eeddf062d3d146a

  # Ilmir: remove $$forInline; should we cherry pick
  # git cherry-pick -x 4fa8266606584a574c110b79a0186c5f2d9835d6 &&

  # Alex: -Xno-use-ir f45ca7acd38223ad428f45cefee209342dbee2ae

  git cherry-pick -x 6fe214d825863576e4537952080fb2aeda92899f &&

  # Alex: More no-use-ir: c3d5a88e52a8fd9adadf17513ebec0f88f02a410

  git cherry-pick -x 86996bf54605680bee80e56c7612f73c1e4d28c3 &&
  git cherry-pick -x 79d7335b8d0e5c70331c3a144a0b1f403df2b36d &&

  # Alex, Throws attribute.
  # git cherry-pick -x 621936e95156eb75489fe05a7967958b37ede4e0

  git cherry-pick -x 0815ed2cbd07ea9fd1e5c85a547ef51da4f2d444 &&
  git cherry-pick -x 6e6f4d050361c13fd1198806ef52b9d20a164c20 &&
  git cherry-pick -x 82ddd700ce88dd2f9f3eb86ad339c4ffa1d2a1df &&

  # Alex, muting language level 1.4 tests:
  # 26f435eb9021489ae84f76ec69a6956a64ca2682

  git cherry-pick -x d5ff1047a5573a63e15855cf3ce65470dc0e3e19 &&
  git cherry-pick -x f8341ad7ebb1ea97ab356a38aa92998ecd67eaed &&

  git cherry-pick -x c027c0e659a91e109df3d10042d7cc581ecab90f &&
  git cherry-pick -x 8746d08dd555337e2ff2913c3cdd0e6a4f88c748 &&
  git cherry-pick -x a49ed1eca2db98a21aaac53782ba2056aca5f698 &&
  git cherry-pick -x 174b3db723209a014918c355e604c4aec1f34a34 &&
  git cherry-pick -x 33a24bfd27838f3dd8b0e836629e6ccdea7ac6b5 &&
  git cherry-pick -x 1ed7e33f4204923a0811cef499ba52b1748718d2 &&
  git cherry-pick -x 4ef2ecf9a9b2327b5c3c1b8ae1b44c0aeaa261d0 &&
  git cherry-pick -x 64c1446fbe75abf2d5337d1688feaa6d28156720 &&
  git cherry-pick -x 33c0bfb4c2cca2fa49e7eac8368cafbf9c4bff9c &&
  git cherry-pick -x 7167d5f75c01ed1992ad126b0994862fa7ba6a25 &&
  git cherry-pick -x 2507e2b526ff6b07d011a79efc818a2d57d12fd4 &&
  git cherry-pick -x d17afddaa992ac5194be2207d9c7121133dcf5fa &&
  git cherry-pick -x 4d9d62ad1284e7c412cd27032bf90de03297dfa3 &&
  git cherry-pick -x 16d63cd1d09dc3e27984282d279fb1e3a1b17bc5 &&
  git cherry-pick -x 9f5b51ed43e8ea2d5cbfd120a27790d3d2a12fb6 &&
  git cherry-pick -x 56c8fdc6c4c133a3d7c9f0dcd2cfce7398004bac &&
  git cherry-pick -x 891a55d79add57372ffedbae96c0f4964be72ed2 &&
  git cherry-pick -x 877509306debf46e54df3e4c9f5fac8f697b87f2 &&
  git cherry-pick -x 6ede10c1ca102e7035bbac1a790a69cfd4c69e30 &&
  git cherry-pick -x 1ef5e25c6088485267f45a004ea9e462df97fbe3 &&
  git cherry-pick -x 1c527fc1591b6a9289d8c9fb0889bbb903d5d20f &&
  git cherry-pick -x 79840a05b23f576cbb830ee7f3fe46d57372def8 &&
  git cherry-pick -x 8e074828625f8b131590ab69c1d6349b0b15572c &&
  git cherry-pick -x a55989a2a52ff924c8f254d360736439bd4dd182 &&

  # `fun interface` support, do not want that on 1.3.70.
  # git cherry-pick -x 64a405e7a0d5c3ad94c8d32922295d827e58776c &&

  git cherry-pick -x 2f0f4e570f1ca2cdb5f879bcc0e7324e7c01e344 &&
  git cherry-pick -x aea5e3ffbcb698b166c1c8759d6b673c2a24b0dd &&
  git cherry-pick -x 7329a1641a2f012dc0700ec99a65217a85180b1c &&

  # FE change for adapted callable references. Should we
  # take this?
  # git cherry-pick -x 89c832b5a012077b493dc4aca5bd03cf676905e9 &&

  # Adapted callable references. Should we take this?
  # git cherry-pick -x c5f14a29a4774037d54c435a593061aab7f5d252 &&
  # git cherry-pick -x 38b90b7fbd84b7a8a9cbf21a30e3397e0911790b &&
  # git cherry-pick -x 57bbfbbfccb1742be8014ac166d76aaefa2b81f6 &&
  # git cherry-pick -x c540116b71296c006dec6c971cbe292912e121b9 &&

  git cherry-pick -x 4094841dc6d84a87e4cc9949c2bbb94f997f2f86 &&
  git cherry-pick -x ca3c1d04c59eac04fb5427907c5a0c019ade9f79 &&

  # psi2ir inner class constructor callable reference gen.
  # git cherry-pick -x 90b250d2410f6b1f84b5269e40cf3976230438dd &&

  # psi2ir error reporting abstraction.
  # git cherry-pick -x dc4d453879e6e88ba764a10631632368fd2953fc &&

  git cherry-pick -x e327b174a22ff4f50023d4267f9ae43bd313601f &&

  # resolving conflicts caused by fir in build files by picking ours
  git cherry-pick --strategy-option=ours -x 6e94eddb71fc74c2361cd0a39a83541258a9165c &&

  # psi2ir use substituted value param in function reference adaptation.
  # git cherry-pick -x 0152f19d5f316297b87c165817c7f5400106fd89 &&
  # git cherry-pick -x c939fb7b0570beba9ab0acf884e51a25cc4152dcq &&

  # fir.txt update, just take the current one and regenerate at the end.
  git cherry-pick --strategy-option=ours -x f1669e22300aeb72e651295d2ba58028082bcc9d 
  git cherry-pick -x ed4be36484c1363ff1867eaff6a75daa0ce744ea &&
  git cherry-pick -x 47d6bdfd35a021b6486c06c4d881f638abdc7a4b &&

  # Reading klib. Don't care.
  # git cherry-pick -x 5ede65c5255ed0220b0edac8b38d2b424d4acf15 &&
  # git cherry-pick -x f1b5198b8609e6f8958dfeed1ad5a035bf3100f8 &&
  # git cherry-pick --strategy-option=theirs -x 8f4b4007fec145e0c763a3132c38a971f6ce575d &&
  # git cherry-pick -x f75400cc1a9323de42a3a9d9adcb57c8dea89bf2 &&
  # git cherry-pick -x 97abc872b256b8d5a5d0756a227d2a02b5de1ec5 &&
  # git cherry-pick -x e351d560d671665dcdc1bbdb8ca25893567b1169 &&

  # apply and revert change
  # git cherry-pick -x 62f9e7a810f4ba1f46b7dc9501836dd30a610893 &&
  # git cherry-pick -x f256547cc8692c7b57c8e6f9c443655e31d70a23 &&

  # psi2ir sam conversion once for index variables
  #  git cherry-pick -x bf9673a0a2779f5ba6429bc3105ad580620f3c31 &&

  # support type annotations
  # git cherry-pick -x 6c07dbf351c7dcc3abefca3ee6e2d03afdaeb9a4

  git cherry-pick -x c42984ca336abf6c49cf7466c1b14138ebf33eb6 &&

  git cherry-pick -x 17e89fbbdb91d8b0e5e4c98cdb213e8f170e1d2d &&
  git cherry-pick -x 504d79577dfec8376e1fe9f3a39082c751fa578a &&
  git cherry-pick -x 1ecf9d407fd2da5ed4f712975e955aa659703b10 &&
  git cherry-pick -x 342ff50e31c1ff1c8db1f7b43ee7f1938355f2d1 &&
  # TODO: Type annotation tests are failing here which is to
  # be expected as it has not been cherry-picked. Mute them.
  git cherry-pick -x f5f25224b057714a8a03b4120a4bb32b6e8c9ef6 &&
  git cherry-pick -x e226561150c2f36c4af9a292d21bfe80b0860e75 &&
  git cherry-pick -x f262f61096752f01055ced7b01c823a46164a219 &&
  git cherry-pick -x 73aa36ca59c38f4cf73d09c09a95b0af763ee8fb &&

  # Generate JvmOverloads as final. 1.4?
  # git cherry-pick -x dcf6a2199ac10fc6b601cd2cd7e894b4a3b4aefa &&
  # git cherry-pick -x d71dec9b642615350ef67e8419de6b1afaf59561 &&

  # MustBeDocumented annotation.
  git cherry-pick -x 186a456e016a3e3c8c5780397fdf17610514b8a2 &&

  # psi2ir sam conversion in varargs
  # git cherry-pick -x 53f66e95090b28f5368236191e34c6e065759393

  # References a test file that doesn't exist, so regenerate tests
  # after cherry-picking.
  git cherry-pick --strategy-option=theirs -x 10900e0d907d33697dfc26ae0a84389e0bc8291f &&
  ./gradlew compiler:generateTests &&
  git commit -a -m "Regenerate tests." &&

  git cherry-pick -x 61e6d346aa02baab0ccdcca096d1366c6ec51603 &&
  git cherry-pick -x cf3e4608f3b605711ebe88c73aa78d0604c2734b &&
  git cherry-pick -x d68a1898d0135e947ecdbebd39003b8ceaf8f57f &&
  # already merged.
  # git cherry-pick -x 3ee344b836cba82052ab8028145d12e055402720 &&
  git cherry-pick -x cd0c45c8328f41f2b2e22d18f16acfb97d8bf4dc &&

  # Names for enum name and ordinal local variables.
  # git cherry-pick -x 9e264916314e6bf2b064383fe68607f4ce82afff &&

  git cherry-pick -x bda5b0d5a991481faf9de7328aee6dec5a17e64a &&

  # psi2ir BE diagnostics
  # git cherry-pick --strategy-option=theirs -x 8ef79f932c8fbbc0fce04a6aabe83769eea6012d &&

  git cherry-pick -x e42a4b2fac7b076b8e8ecd1f78642de98739608d &&

  git cherry-pick -x 00de5dae326fde9e67679641ae7d40172d877128 &&
  git cherry-pick -x 19b516cbf4ea949d1a36fe341d50245cc185f510 &&
  git cherry-pick -x 451fa245b83f31d04066ff8a6286ce1aa48f71cd &&
  git cherry-pick --strategy-option=theirs -x 6ecda9e8afd9be5a733898816f6933fe09ea15ad &&

  git cherry-pick -x 645488b342911b2b7392190c36ef9e7f0aa63301 &&
  git cherry-pick -x d6f91333dc1edad97bc7d849a6823f1876ad1451 &&
  # empty
  # git cherry-pick -x 562e8e357aa9dd294a388909f07aa6a0114805bb &&
  # enabling some tests - conflicts because of missing files
  # git cherry-pick -x 8214a9383cd00b383acda26af5ba59f207da242a &&
  git cherry-pick -x 3606a4104b85e5f804ee794677c652c1814fd553 &&
  git cherry-pick -x 4dd794c2d2369ff80642cb8f7c7baf99ea22e3ca

  # Resolve in favor of version that doesn't deal with type
  # annotations.
  git cherry-pick --strategy-option=ours -x fc70455877aa4b21696d84b41635c3eee2f55ddc &&
  git cherry-pick -x e5bd4f74f3c46e574b49cf23f1bcde78c2ce7517 &&
  git cherry-pick -x 5977799c59584c1893d468f1ab1dfa32a9e5f775 &&
  git cherry-pick -x 8335ad7e984bf62b538967a3eec2fc7c2768b1e6 &&
  git cherry-pick -x bc9edea5279c6d5c70fbc3266f3a7dfc16b3a23e &&
  # patch up FunctionCodegen functionView -> irFunction.
  git apply patches/patch-function-codegen-function-view.txt &&
  git commit -a -m "Patch function codegen functionView -> irFunction" &&
  
  # WrappedDescriptor isFun 
  # git cherry-pick --strategy-option=theirs -x 1ae001740113e4f835b0af65c742a1eaf8757da0 &&

  git cherry-pick --strategy-option=theirs -x 763cb6dd6fc70a7a9c8555d73156913d24340988 &&

  git cherry-pick -x 5d766eace405997a135502c5eed46276d78d9f51
  git add compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/expressions/IrCallableReference.kt &&
  git add compiler/ir/backend.jvm/src/org/jetbrains/kotlin/backend/jvm/lower/InterfaceLowering.kt &&
  EDITOR=ls git cherry-pick --continue &&
  git apply patches/interface-lowering-changes.txt &&
  git commit -a -m "Patch interface lowering." &&

  git cherry-pick -x 18dcbb3c94694c7fa3e203900a98198b877b8719 &&
  git cherry-pick -x a4cc5ea1da9a7607d837a589aa2e70783b9fca38 &&

  # Ilmir: Spill stack before analyzing it when looking for non-inline suspend lambda
  # git cherry-pick -x 7dfd7b6081ce2f16011b4178b46de87db7a94695 &&

  # Anton's IR API changes. Let's try not to do these for now.
  # git cherry-pick --strategy-option=ours -x 0bcde9dffcfa631f3b898f6dc6cfaf0c0ea8a66a &&
  # git cherry-pick --strategy-option=ours -x e8fba8bcb68c4b84396674b7d5e472d1860754ac &&
  # git cherry-pick --strategy-option=theirs -x 20dc3ccdb8e0debdbb65fecd3ce20fc312894296 &&
  # git apply patches/no-psi-error-builder.txt &&
  # git commit -a -m "Patch JvmBackendContext to have no PsiErrorBuilder." &&
  
  git cherry-pick -x 08074bb60e8d3647681314e7903fea66e952f996 &&
  git cherry-pick -x 2bf50cc91aaf92e62efc1068f9742e22fe6133f8 &&
  git cherry-pick -x 5acb3e14fb811ab63b1a3ae2d090d7ace088c19a &&
  
  git cherry-pick -x 06408011f0f99b104658260c8bedaf75e21fa025
  git add compiler/ir/backend.jvm/src/org/jetbrains/kotlin/backend/jvm/lower/AddContinuationLowering.kt &&
  EDITOR=ls git cherry-pick --continue &&
  git apply patches/add-continuation-lowering.txt &&
  git commit -a -m "Fixup AddContinuationLowering." &&

  git cherry-pick -x 8cdef13537faaba9243b7d181df7f0a8e587573a &&

  git cherry-pick -x 73b4f897b6fdc480b4fdd0f5839081c726da7d25 &&

  git cherry-pick -x 66cbe3b1a8e1d73e82ddbfc096191839b551758d
  git add compiler/ir/backend.jvm/src/org/jetbrains/kotlin/backend/jvm/lower/inlineclasses/MemoizedInlineClassReplacements.kt &&
  EDITOR=ls git cherry-pick --continue &&
  git apply patches/memoized-inline-class-replacements.txt &&
  git commit -a -m "Fixup MemorizedInlineClassReplacements." &&

  git cherry-pick -x 30166b20b74b35adb95e7a1c5b18257ff013c20b &&
  git cherry-pick -x edd0ac6c6f14b42b934b6634330aeab32b76299c &&
  git cherry-pick -x f46ad102664d6da01af5278cd65e4916bee838ff &&
  git cherry-pick -x a4414829bc620912a1f5df1b8fec5022fd945bb4 &&
  git cherry-pick -x 0e243ca29594b066860da8b1ae662d9b05db4c0e &&

  git cherry-pick -x 12e31a17603282699f500094597ce3a33694a89c 
  git add compiler/ir/backend.jvm/src/org/jetbrains/kotlin/backend/jvm/lower/inlineclasses/MemoizedInlineClassReplacements.kt &&
  EDITOR=ls git cherry-pick --continue &&
  git apply patches/memoized-inline-class-replacements2.txt &&
  git commit -a -m "Fixup MemorizedInlineClassReplacements." &&

  git cherry-pick --strategy-option=theirs -x 5f6af58aeb5f53ca63a57e430464c6d3f1db173e &&
  git cherry-pick -x 461d8ef368bc165a5db7185b829990657ccc5cfe &&

  git cherry-pick --strategy-option=theirs -x 07737f8fc6d014ebc37bcd4a6075820c4de2e97a &&
  git apply patches/bridge-lowering.txt &&
  git commit -a -m "Fixup BridgeLowering." &&
  # git says this is empty, not so sure about that...
  # git cherry-pick -x 951b2fa770633164dbeedceb9b3c4ec2a2a1ebe1 &&

  git cherry-pick -x 304700cf7bcd8ef481c2ec2ba428929d76e10957 &&
  git apply patches/bridge-lowering2.txt &&
  git commit -a -m "Fixup BridgeLowering." &&

  git cherry-pick -x 182e1c1b3bbbb350181962d3dcf973daced40fb9 &&
  git cherry-pick -x f7c784adebcc9144f735c4027c37bb72e5495827 &&

  git cherry-pick -x 15ff74209ccc2225343667dea127ab1083826c52 &&

  git cherry-pick -x 17e1f081c75adfd4826f50fca0cc27fc066a1255 
  git rm compiler/tests/org/jetbrains/kotlin/checkers/DiagnosticsTestWithJvmIrBackendGenerated.java &&
  git rm compiler/tests/org/jetbrains/kotlin/checkers/DiagnosticsTestWithOldJvmBackendGenerated.java &&
  EDITOR=ls git cherry-pick --continue &&

  git cherry-pick -x 058b2295440724cc072aee819b8b09addf0bfc24 
  git add compiler/ir/backend.jvm/src/org/jetbrains/kotlin/backend/jvm/lower/JvmStaticAnnotationLowering.kt &&
  EDITOR=ls git cherry-pick --continue &&
  git apply patches/static-annotation-lowering.txt &&
  git commit -a -m "Fixup StaticAnnotationLowering." &&

  git cherry-pick -x 6584df3e01e0bb5f36d02fea02846cdb17af3b92 &&
  git cherry-pick --strategy-option=ours -x 9db82bfcc81fc49f5cb266bd1769620c5a2bcaf0 &&
  git apply patches/ir-utils.txt &&
  git commit -a -m "Fixup IrUtils." &&

  git cherry-pick --strategy-option=theirs -x 6d23e50142df38fd1596fa68771457eb58e86933 &&

  git cherry-pick -x ba606147c95edb9e3988654fb44bbe1ac4b24bfe &&
  git cherry-pick -x 89cf32eccc813b10b506799ae132f1fb5dbf03a9 &&

  # Error reporting for signature clases.
  # git cherry-pick -x 866f188120524d0034e52042a7f6ae22e22e0ec2 &&

  git cherry-pick -x 3278451b073b683fa04f290c20f14c60914e0797 &&
  git cherry-pick -x 76f8109ff60fdc7bd20395f29665ef0193e8f755 &&

  git cherry-pick -x f782ea075ba6590911573a0301154be435b2198f
  git rm compiler/testData/codegen/bytecodeText/companion/protectedCompanionObjectAccessors_after.kt &&
  EDITOR=ls git cherry-pick --continue &&

  git cherry-pick --strategy-option=theirs -x 5a49ccac760d479fd3830d4dc0823448c9d952ea &&
  git apply patches/arguments-utils.txt &&
  git commit -a -m "Patch arguments utils." &&

  git cherry-pick --strategy-option=theirs -x 06b6477d044d10779f68189909ed76346e97f78c &&
  git cherry-pick -x c71e87068a97894763bf0e15b0434a3fce0c2e78 &&
  git cherry-pick -x 79060e7f40d89c009294652cadf7ded4402add87 &&

  # More API stuff. Ignore?
  # git cherry-pick -x 0254734bb53150aa75e8d5008b96800c2e0fdfa5 &&
  # git cherry-pick -x d51b14fe3cf54d60c30a4724e05eac973b0db833 &&
  # git cherry-pick -x 45f036548b9d76b8fce296e668ffa2addb042b72 &&
  # git cherry-pick -x bb04eae93e79e1963436ecb9208912af1a192c44 &&
  # git cherry-pick -x cef9ed1dae6943d5d94b1c175f5994ccba9cad5e &&
  # git cherry-pick -x 31d73c5d79548135daa786d988098ac313220ac6 &&
  # git cherry-pick -x 5004bb363601eb8739bd2dd5aebaa0ea52ca9627 &&

  git cherry-pick -x bc2b96f6344729f632229647d99561bafdaa7822

  ###### vvvvv UNMERGED vvvvv ######

#  git cherry-pick -x 2b2ae8a5f1c85b7f08ba4157a92590b04ca6f34d &&
#  git cherry-pick -x 5760c0be9cdeffb00cee05711b30aff6aa1c586a &&
#  git cherry-pick --strategy-option=theirs -x 64141b8b38bde486478aa9fb72c1bb671ad89782 &&
#  git cherry-pick -x 4b954c347a189dd61eb5ddb98f502f525c749a95 &&
#
#  # psi2ir
#  git cherry-pick -x 6d1da6e6d58fb86d423a01ba4a7acc8724cf69b3 &&
#
#  git cherry-pick -x 752ff9de5d269dd7cbe937501eddcf166e1034e6 &&
#  git cherry-pick -x 5d603a8be4106d6be9694fd9caf0eeae95919283 &&
#  git cherry-pick -x e6efb81014e03258f8edef5f779b599f915df8c6 &&
#  git cherry-pick -x 272f6abe69d3348666bd39e3179e1d6556ade709 &&
#  git cherry-pick -x 3080b4c435b997a89c47c509786568e3ce15ab54 &&
#  git cherry-pick -x 3d51af293516240db2311fcced8bac4bc3703c7f &&
#  git cherry-pick -x 0ed719f7924076dedb10f81e96e3f3e96dcf9adb &&
#  git cherry-pick -x 3cf71c1d2b430c4e6d562fd3fbcc763f9b36eab9 &&
#  git cherry-pick -x eff02b6e723ba1bb01bbb83596073816de8039ab &&
#  git cherry-pick -x 55aafb3430a2aaf0f48292d47e0cd1a2cc3b9378 &&
#  git cherry-pick -x 5b62c9e54dcbf634c0b323527a887f8a43c8dd84 &&
#  git cherry-pick -x 8c4eef9844dc8068480bf31244bcebc01d8ada20 &&
#  git cherry-pick -x 325ad14ac9b3bffd8d2825df370435357045e470 &&
#  git cherry-pick -x 52e7cd57253d30753db7e1aff56e89a295b7b0a4 &&
#  git cherry-pick -x 6b98ea2378bfbc4401fa9a1b8752f8db520e8d8e &&
#  git cherry-pick -x c5ffbfd33c4c65fdd088df12f09db380739276b9 &&
#
#  # psi2ir intersection types
#  git cherry-pick -x e9a7be4a4667e93ca51c4081f0be2e1869c7501f &&
#
#  git cherry-pick -x d982203d56b8637a0e300b25fb33d9f9c5bdab2d &&
#  git cherry-pick -x a3d85e108f4b4a2f0b956a08c10d084731c215df &&
#  git cherry-pick -x fe71d5256c30524f3275d6c1bfdfafa569c3091e &&
#  git cherry-pick -x 2b6a0d6c58d4c8ca3084bdb2b75068cf29171f79 &&
#  git cherry-pick -x c1d350f8f37883a004c7a66040b557a26a3df1ce &&
#
#  # psi2ir patch parents
#  git cherry-pick -x dff7d7b7b988ebe28d20c3a9e7fce30db367ec2b &&
#
#  # psi2ir support String?.plus(Any?)
#  git cherry-pick -x ad0070ed8a6c1468a5321194150e27fefd6c479d &&
#
#  git cherry-pick -x 035cc57cf47054383a44e668612927dd793f9d22 &&
#  git cherry-pick -x 3881220386e50857a9542f8c919d0f2f70879c55 &&
#  git cherry-pick -x 456139fc5e7e890963298ad8cc9bf158e179107f &&
#  git cherry-pick -x 963258189a36a9866b4e483628653cba4e8a1776 &&
#  git cherry-pick -x 8c9ebc1bf902ee82ea0c2c0256d2052330f86e67 &&
#  git cherry-pick -x 465e9f2d6863898c2d1c98a15087e333672877a8 &&
#  git cherry-pick -x 4ac45eb38be60d6db7abd1c8149e424cd6531e8d &&
#  git cherry-pick -x 2010d8d2b91f865f3e10d2de6b16e7fa6a8553bb &&
#  git cherry-pick -x 536e0e23a0f92375a91329351625058c645c2188 &&
#  git cherry-pick -x 42420cb6fce8e5623da324e7fe1ff45f90f0c467 &&
#  git cherry-pick -x 22de20e7e53bf5ca56e04fe5d1f2c8c4d6a15283 &&
#  git cherry-pick -x 7e6d0801235a76f3a74836ee1ac6432d1f4a0e3a &&
#
#  # psi2ir use resulting descriptor extension receiver type
#  git cherry-pick -x 01c4e66edbf1121444b8acfccaae29f8e38ea216 &&
#
#  git cherry-pick -x 6e40117116eeaf43893dae54fbe1312a4b13bb95 &&
#  git cherry-pick -x d84a6fa7423dbd5ebbd3f1d3c14afde867bf828e &&
#  git cherry-pick -x 159d292ea7912471d7725d3e4b66b9c71bad4e40 &&
#  git cherry-pick -x 5896d8003ecc8e782b70baf89f167f922bf5f4e7 &&
#  git cherry-pick -x 6184171a11346cdb767a35a2bc16bcdee53f4823 &&
#  git cherry-pick -x f66b99494637bbc21bdb254bc571fd461a990e20 &&
#  git cherry-pick -x f678db2f8950b79939baf48dfc86afb759b9fdd0 &&
#  git cherry-pick -x dd7d5dfdb3aaf07a96be67e05c5ffa83bde42a34 &&
#  git cherry-pick -x 5826db97c7a7b647f05cc20b16a324d182e17471 &&
#  git cherry-pick -x f8903ca04b9d15da44451514a64b895738ebe1a2 &&
#  git cherry-pick -x 4b4a6101c1fa0c93b4dfff1c2567a0e47c4c1d77 &&
#  git cherry-pick -x c94f8d3767dc4739024d826d248e1d60057ad742 &&
#  git cherry-pick -x 354fb3c4ba7682ad68ab79d508e58ea3a5584ee0 &&
#  git cherry-pick -x bb5a639153b483ef130ae2992fb2e73eba4cf5e6 &&
#  git cherry-pick -x 18d95bb670bf18919d11e1d9fc5a3a2680dfa87c &&
#  git cherry-pick -x 56b983e6255dac4d2544afa6b2e13d93db108082 &&
#  git cherry-pick -x 93394656a3096732ed1a5c60bfe6f4b6f18b4f8e &&
#  git cherry-pick -x 8487211ae18cb4a2db6cc8f2e9c3a3e1809385ae &&
#  git cherry-pick -x 68745ec43f198791918ec4b8b1c118ee6067e109 &&
#  git cherry-pick -x 13b04e63be44b56c380e510943d3dc34020ec8b0 &&
#  git cherry-pick -x 037b442e8605d9585a16b46038b2aa7416e4173a &&
#  git cherry-pick -x 20ea77d55d75b71e3feb2a4131080afe1d98bf4d &&
#  git cherry-pick -x a732e8f5fe81370eed14d4a153bc1028c2cef882 &&
#  git cherry-pick -x 368b0d9b0bcdba1ec62d9f2e2552e93714e434d8 &&
#  git cherry-pick -x b393f2f6806190100d2ae0af4eca96acd19d4760 &&
#  git cherry-pick -x 862cd5665b1428fd5d3a2aa30962f52b9ea1fce7 &&
#  git cherry-pick -x 735fae0e5a05340fc3eea9f04b9365e0c4da2495 &&
#  git cherry-pick -x 2239b5ceabd7c9dabe46d1c3665614bf4cf986ad &&
#  git cherry-pick -x fd70b10b17512c4d6ec2339f090c4aff64af71b7 &&
#  git cherry-pick -x fded6fb49432b0082b703a7b7f0fa83dc3e7f850 &&
#  git cherry-pick -x 7efab887aa5953be3fead5a97a2e25118c87917d &&
#
#  # 'this' in delegating constructor
#  git cherry-pick -x c0b15b1768eaade6d9181a48eee3613277996fe5 &&
#
#  git cherry-pick -x 244db9bcf9e1548f2132bf91391506836fe9818e &&
#  git cherry-pick -x 92268c8144db4d0b390a8ef37d021edaf99ec086 &&
#  git cherry-pick -x 426738b7d490b0fd44f610fbec3f09908005dd75 &&
#  git cherry-pick -x bdd88e16553acc165aaf39c07ccf9ead9daee242 &&
#  git cherry-pick -x a1eff7f4af013d165c89f8ede84f46b8936ade38 &&
#  git cherry-pick -x 6a1e35389c818fecf6a32e1acca547e1c704a2bc &&
#  git cherry-pick -x 6809f4439c18f9363846bff29d0a515de7d702e9 &&
#  git cherry-pick -x 2e542da91d376a9afc0443291b13a5e728698946 &&
#
#  # ? 
#  git cherry-pick -x 2851fab281e0deea4e552697f9e7ef66a3fce9da &&
#  git cherry-pick -x ade18d144a09e722b8354405d474d18e0350ebe9 &&
#  git cherry-pick -x 1c24a97b9e8c96e413786c6ac4147e692ab0fed2 &&
#  git cherry-pick -x dd27b3d4f12d23df5ceb9abb073aaf68d50c5a1b &&
#  
#  git cherry-pick -x 2c0650331140b2c5b4176bbc00c43d5b16c0f388 &&
#  git cherry-pick -x 03613d4708719d183180fda844dfa9414421b2b8 &&
#

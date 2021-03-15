#!/usr/bin/env python

import os
import subprocess
import sys

BUILD_VERSION = "1.4.31-google-ir-02"

ARCHIVE_PREFIX = "r8-releases/kotlin-releases"
STORAGE_URL_PREFIX = "http://storage.googleapis.com"

ROOT = os.path.abspath(os.path.normpath(os.path.join(__file__, '..', '..')))
THIRD_PARTY = os.path.abspath(os.path.normpath(os.path.join(__file__, '..', 'third_party')))
JDK7_DIR = os.path.join(THIRD_PARTY, "jdk7", "java-se-7u75-ri")
JDK8_DIR = os.path.join(THIRD_PARTY, "jdk8", "linux-x86")
JDK9_DIR = os.path.join(THIRD_PARTY, "jdk9", "linux")
BUNCH = os.path.join(THIRD_PARTY, "bunch-cli-1.1.0", "bin", "bunch")
COMPILER_ZIP_NAME = "kotlin-compiler-%s.zip" % BUILD_VERSION
COMPILER_ZIP_PATH = os.path.join(ROOT, "dist", COMPILER_ZIP_NAME)
AS_PLUGIN_SRC_ZIP_NAME = "kotlin-plugin.zip"
AS_PLUGIN_TARGET_ZIP_NAME = "kotlin-plugin-%s-as42.zip" % BUILD_VERSION
AS_PLUGIN_ZIP_PATH = os.path.join(ROOT, "build", AS_PLUGIN_SRC_ZIP_NAME)
KOTLIN_PLUGIN_SRC_ZIP_NAME = "kotlin-plugin.zip"
KOTLIN_PLUGIN_TARGET_ZIP_NAME = "kotlin-plugin-%s-ij.zip" % BUILD_VERSION
KOTLIN_PLUGIN_ZIP_PATH = os.path.join(ROOT, "build", AS_PLUGIN_SRC_ZIP_NAME)

class ChangedWorkingDirectory(object):
 def __init__(self, working_directory, quiet=False):
   self._quiet = quiet
   self._working_directory = working_directory

 def __enter__(self):
   self._old_cwd = os.getcwd()
   if not self._quiet:
     print 'Enter directory:', self._working_directory
   os.chdir(self._working_directory)

 def __exit__(self, *_):
   if not self._quiet:
     print 'Enter directory:', self._old_cwd
   os.chdir(self._old_cwd)

def print_cmd(cmd, env=None, quiet=False):
  if quiet:
    return
  if type(cmd) is list:
    cmd = ' '.join(cmd)
  if env:
    env = ' '.join(['{}=\"{}\"'.format(x, y) for x, y in env.iteritems()])
    print('Running: {} {}'.format(env, cmd))
  else:
    print('Running: {}'.format(cmd))
  sys.stdout.flush()

def get_java_env():
  java_env = dict(os.environ, JAVA_HOME=JDK8_DIR)
  java_env['PATH'] = java_env['PATH'] + os.pathsep + os.path.join(JDK8_DIR, 'bin')
  java_env['GRADLE_OPTS'] = '-Xmx1g'
  java_env['JDK_16'] = JDK7_DIR
  java_env['JDK_17'] = JDK7_DIR
  java_env['JDK_18'] = JDK8_DIR
  java_env['JDK_9'] = JDK9_DIR
  return java_env

def download_from_google_cloud_storage(sha1_file, bucket='r8-deps', auth=False):
  download_script = 'download_from_google_storage'
  cmd = [download_script]
  if not auth:
    cmd.append('-n')
  cmd.extend(['-b', bucket, '-u', '-s',  sha1_file])
  print_cmd(cmd)
  subprocess.check_call(cmd)

def download_deps():
  download_from_google_cloud_storage(os.path.join(THIRD_PARTY, "jdk7", "java-se-7u75-ri.tar.gz.sha1"))
  download_from_google_cloud_storage(os.path.join(THIRD_PARTY, "jdk8", "linux-x86.tar.gz.sha1"))
  download_from_google_cloud_storage(os.path.join(THIRD_PARTY, "jdk9", "linux.tar.gz.sha1"))
  download_from_google_cloud_storage(os.path.join(THIRD_PARTY, "bunch-cli-1.1.0.tar.gz.sha1"))

def upload_file_to_cloud_storage(source, destination, public_read=True):
  cmd = ['gsutil.py', 'cp']
  if public_read:
    cmd += ['-a', 'public-read']
  cmd += [source, destination]
  print_cmd(cmd)
  subprocess.check_call(cmd)

def get_destination(name):
    return "gs://%s/%s/%s" % (ARCHIVE_PREFIX, BUILD_VERSION, name)

def get_download_url(name):
    return "%s/%s/%s/%s" % (STORAGE_URL_PREFIX, ARCHIVE_PREFIX, BUILD_VERSION, name)

def build_and_upload_compiler():
  cmd = [
      "./gradlew",
      "-Pteamcity=true",
      "-Pbuild.number=%s" % BUILD_VERSION,
      "zipCompiler"
  ]
  with ChangedWorkingDirectory(ROOT):
    print_cmd(cmd)
    subprocess.check_call(cmd, env=get_java_env())
  upload_file_to_cloud_storage(COMPILER_ZIP_PATH, get_destination(COMPILER_ZIP_NAME))
  print "Uploaded to: %s" % get_download_url(COMPILER_ZIP_NAME)
  git_reset()

def bunch_switch(target):
  git_reset()
  cmd = [BUNCH, "switch", target]
  with ChangedWorkingDirectory(ROOT):
    print_cmd(cmd)
    subprocess.check_call(cmd)

def bunch_restore():
  git_reset()
  cmd = [BUNCH, "restore"]
  with ChangedWorkingDirectory(ROOT):
    print_cmd(cmd)
    subprocess.check_call(cmd)

def git_reset():
  cmd = ["git", "reset", "--hard"]
  with ChangedWorkingDirectory(ROOT):
    print_cmd(cmd)
    subprocess.check_call(cmd)

def build_and_upload_kotlin_plugin():
  cmd = [
    "./gradlew",
    "clean",
    "cleanupArtifacts",
    "ideaPlugin",
    "-Pteamcity=true",
    "-Pbuild.number=%s" % BUILD_VERSION,
    "-PdeployVersion=%s" % BUILD_VERSION,
    "-PpluginVersion=%s-release-IJ" % BUILD_VERSION
  ]
  with ChangedWorkingDirectory(ROOT):
    print_cmd(cmd)
    subprocess.check_call(cmd, env=get_java_env())
  cmd = [
    "./gradlew",
    "writePluginVersion",
    "zipPlugin",
    "-Pteamcity=true",
    "-Pbuild.number=%s" % BUILD_VERSION,
    "-PdeployVersion=%s" % BUILD_VERSION,
    "-PpluginVersion=%s-release-IJ" % BUILD_VERSION
  ]
  with ChangedWorkingDirectory(ROOT):
    print_cmd(cmd)
    subprocess.check_call(cmd, env=get_java_env())
  upload_file_to_cloud_storage(KOTLIN_PLUGIN_ZIP_PATH, get_destination(KOTLIN_PLUGIN_TARGET_ZIP_NAME))
  print "Uploaded to: %s" % get_download_url(KOTLIN_PLUGIN_TARGET_ZIP_NAME)
  git_reset()

def build_and_upload_android_studio_plugin():
  bunch_switch("as42")
  cmd = [
    "./gradlew",
    "clean",
    "cleanupArtifacts",
    "ideaPlugin",
    "-Pteamcity=true",
    "-Pbuild.number=%s" % BUILD_VERSION,
    "-PdeployVersion=%s" % BUILD_VERSION,
    "-PpluginVersion=%s-release-Studio4.2-1" % BUILD_VERSION
  ]
  with ChangedWorkingDirectory(ROOT):
    print_cmd(cmd)
    subprocess.check_call(cmd, env=get_java_env())
  cmd = [
    "./gradlew",
    "writePluginVersion",
    "zipPlugin",
    "-Pteamcity=true",
    "-Pbuild.number=%s" % BUILD_VERSION,
    "-PdeployVersion=%s" % BUILD_VERSION,
    "-PpluginVersion=%s-release-Studio4.2-1" % BUILD_VERSION
  ]
  with ChangedWorkingDirectory(ROOT):
    print_cmd(cmd)
    subprocess.check_call(cmd, env=get_java_env())
  upload_file_to_cloud_storage(AS_PLUGIN_ZIP_PATH, get_destination(AS_PLUGIN_TARGET_ZIP_NAME))
  bunch_restore()
  print "Uploaded to: %s" % get_download_url(AS_PLUGIN_TARGET_ZIP_NAME)
  git_reset()

def main():
  download_deps()
  build_and_upload_compiler()
  build_and_upload_kotlin_plugin()
  build_and_upload_android_studio_plugin()

if __name__ == '__main__':
  main()

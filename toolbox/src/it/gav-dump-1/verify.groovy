String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('Toolbox ' + projectVersion)
assert buildLog.contains('Maven version ' + mavenVersion)
String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('org.apache.maven:maven-core:jar:3.6.3')

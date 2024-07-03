String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('org.apache.maven:maven-core:jar:3.6.3 EXISTS')
assert buildLog.contains('Checked TOTAL of 5 (existing: 5 not existing: 0)')

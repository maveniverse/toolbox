String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('Outdated versions with known age')
assert !buildLog.contains('Total of 0.00 years from 0 outdated dependencies')
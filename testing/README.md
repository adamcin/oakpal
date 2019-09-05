# oakpal-testing

A simple set of utilities to support testing oakpal logic.

1. `TestPackageUtil`: useful for creating jars and filevault zip files.

2. `oakpaltest.Handler`: java URLStreamHandler implementation that resolves URLs as file paths relative to the test 
execution working directory.
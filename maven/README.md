oakpal-maven-plugin
===================

_TL;DR:_ Paste the following into a content-package pom.xml and get started with oakpal-maven-plugin.

    <plugin>
      <groupId>net.adamcin.oakpal</groupId>
      <artifactId>oakpal-maven-plugin</artifactId>
      <version>2.2.2</version>
      <executions>
        <execution>
          <goals>
            <goal>scan</goal>
          </goals>
        </execution>
      </executions>
    </plugin>


If you develop code for AEM, then you probably build and install content-packages from source using Maven. You might
also be familiar with the fact that the process of installing such a package on AEM can be prone to both
non-deterministic failures and less-than-complete "successes".

What you may not know is that, other than the traditional culprits of lack of disk space or permissions, or the
occasional Package Manager bundle restart, the vast majority of package installation failures are caused by a capricious
gremlin that lives in the packages themselves, and that gremlin's name is "DocView".


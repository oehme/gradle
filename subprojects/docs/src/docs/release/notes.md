The Gradle team is excited to announce Gradle @version@.

This release features [1](), [2](), ... [n](), and more.

We would like to thank the following community contributors to this release of Gradle:

[Danny Thomas](https://github.com/DanielThomas),
[Daiki Hirabayashi](https://github.com/dhirabayashi),
[Sebastian Schuberth](https://github.com/sschuberth)

<!-- 
Include only their name, impactful features should be called out separately below.
 [Some person](https://github.com/some-person)
-->

## Upgrade Instructions

Switch your build to use Gradle @version@ by updating your wrapper:

`./gradlew wrapper --gradle-version=@version@`

See the [Gradle 6.x upgrade guide](userguide/upgrading_version_6.html#changes_@baseVersion@) to learn about deprecations, breaking changes and other considerations when upgrading to Gradle @version@. 

For Java, Groovy, Kotlin and Android compatibility, see the [full compatibility notes](userguide/compatibility.html).

<!-- Do not add breaking changes or deprecations here! Add them to the upgrade guide instead. --> 

<!-- 
Add release features here!
## 1

details of 1

## 2

details of 2

## n
-->

## Improved handling of ZIP archives on runtime classpaths
Runtime classpath analysis can now inspect manifest and `META-INF` properties files, ignore changes to comments, and selectively ignore attributes or properties that don't impact the 
runtime classpath.
   
```groovy
normalization {
    runtimeClasspath {
        metaInf {
            ignoreAttribute("Implementation-Version")
            ignoreProperty("timestamp")
        }
    }
}
```

This improves the likelihood of [build cache hits](userguide/build_cache.html) when jar and property files on the classpath are regenerated and only differ by unimportant values or comments.
 
See the [userguide](userguide/more_about_tasks.html#sec:meta_inf_normalization) for further information.  Note that this API is incubating and will likely change in future releases as support 
is expanded for normalizing properties files outside of `META-INF`.

## Improvements for plugin authors

### New ZIP and TAR `FileTree` factory methods

Previously, it was only possible to create a `FileTree` for a ZIP or TAR archive by using the APIs provided by a `Project`.
However, a `Project` object is not always available.

The `ProjectLayout` service now has [zipTree()](javadoc/org/gradle/api/file/ProjectLayout.html#zipTree-java.lang.Object-) and [tarTree()](javadoc/org/gradle/api/file/ProjectLayout.html#tarTree-java.lang.Object-) methods for creating read-only `FileTree` instances respectively for ZIP and TAR archives.

See the [user manual](userguide/custom_gradle_types.html#service_injection) for how to inject services and the [`ProjectLayout`](javadoc/org/gradle/api/file/ProjectLayout.html) api documentation for more details and examples. 

## Promoted features
Promoted features are features that were incubating in previous versions of Gradle but are now supported and subject to backwards compatibility.
See the User Manual section on the “[Feature Lifecycle](userguide/feature_lifecycle.html)” for more information.

The following are the features that have been promoted in this Gradle release.

<!--
### Example promoted
-->

## Fixed issues

## Known issues

Known issues are problems that were discovered post release that are directly related to changes made in this release.

## External contributions

We love getting contributions from the Gradle community. For information on contributing, please see [gradle.org/contribute](https://gradle.org/contribute).

## Reporting Problems

If you find a problem with this release, please file a bug on [GitHub Issues](https://github.com/gradle/gradle/issues) adhering to our issue guidelines. 
If you're not sure you're encountering a bug, please use the [forum](https://discuss.gradle.org/c/help-discuss).

We hope you will build happiness with Gradle, and we look forward to your feedback via [Twitter](https://twitter.com/gradle) or on [GitHub](https://github.com/gradle).

# Ember.js Plugin

This plugin provides build time compilation for [Ember.js](https://github.com/emberjs/ember.js) handlebar templates.

# How to install

* play 2.2.x

```
addSbtPlugin("com.ketalo.play.plugins" % "emberjs" % "1.2.0-SNAPSHOT")
```

You may need to add a reference to the Sonatype repository
```
resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
```

Or for snapshots

```
resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
```

* play 2.1.x:

```
addSbtPlugin("com.ketalo.play.plugins" % "emberjs" % "1.3.0-SNAPSHOT")
``` 

to your plugin.sbt

# How to Use

* Select your ember version in your Build.scala. Currently supported versions include 1.3.0, 1.2.0, 1.1.2, 1.0.0, 1.0.0-rc.8, 1.0.0-rc.7, 1.0.0-rc.6, 1.0.0-rc.5, 1.0.0-rc.4, 1.0.0-rc.3, 1.0.0-rc.1 and 1.0.0-pre.2

```scala
  import com.ketalo.play.plugins.emberjs.EmberJsKeys
  import sbt._

  object ApplicationBuild extends Build with EmberJsKeys {

    val appName         = "play-emberjs-sample"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq.empty

    val main = play.Project(appName, appVersion, appDependencies).settings(
      emberJsVersion := "1.3.0"
    )

  }
```

* Or if you prefer using build.sbt:

```
name := "<My app name>"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

emberJsVersion := "1.3.0"

play.Project.playScalaSettings
```

* Include ember.js and the corresponding jQuery and handlebars files. Note that they are not provided by the sbt plugin. Check the ember site for them: [ember.js](https://ember.js) 
```html
    <script src="@routes.Assets.at("javascripts/jquery-1.8.2.min.js")" type="text/javascript"></script>
    <script src="@routes.Assets.at("javascripts/handlebars-v1.3.0.js")" type="text/javascript"></script>
    <script src="@routes.Assets.at("javascripts/ember.min.js")" type="text/javascript"></script>
```

* Put your handlebar template (.handlebars) files under the ```app/assets/templates``` directory

* Reference the generated .js in a  ```<script>``` tag:
```html
<script src="@routes.Assets.at("templates/templates.pre.js")"></script>
```

The generated templates.pre.js has the javascript code containing all the precompiled templates in that directory

* **OR** Reference the minified .js in a  ```<script>``` tag:
```
<script src="@routes.Assets.at("templates/templates.pre.min.js")"></script>
```

# Sample

For an example, see the bundled sample app for three different ember versions

* [ember 1.3.0](/sample-1.3.0)
* [ember 1.2.0](/sample-1.2.0)
* [ember 1.1.2](/sample-1.1.2)
* [ember 1.0.0](/sample-1.0.0)
* [ember 1.0.0-rc.8](samples-pre-1.0/sample-1.0.0-rc.8)
* [ember 1.0.0-rc.7](samples-pre-1.0/sample-1.0.0-rc.7)
* [ember 1.0.0-rc.6](samples-pre-1.0/sample-1.0.0-rc.6)
* [ember 1.0.0-rc.5](samples-pre-1.0/sample-1.0.0-rc.5)
* [ember 1.0.0-rc.4](samples-pre-1.0/sample-1.0.0-rc.4)
* [ember 1.0.0-rc.3](samples-pre-1.0/sample-1.0.0-rc.3)
* [ember 1.0.0-rc.1](samples-pre-1.0/sample-1.0.0-rc.1)
* [ember 1.0.0-pre.2](samples-pre-1.0/sample-1.0.0-pre.2)

# Acknowledgments

This plugin was based the work from the blog post [Ember/Handlebars template precompilation with Play](http://eng.netwallet.com/2012/04/25/emberhandlebars-template-precompilation-with-play/)

A good portion of the plugin internals were based on the [Dust.js play plugin](https://github.com/typesafehub/play-plugins/tree/master/dust)

# Modifications to ember.js

Ember.js uses modern javascript features that are not properly supported by rhino 1.7R4 which is the one used with the play framework.
This has been documented in [rhino#93](https://github.com/mozilla/rhino/issues/93) and [emberjs#1202](https://github.com/emberjs/ember.js/issues/1202).
The plugin thus need a customized version of the ember-xxx.js file that can be complied by rhino.

The changes required are:

* Ember.js defines a function **ComputedPropertyPrototype.volatile**. **volatile** is a reserved keyword in rhino, so all instances are replaced by **_volatile**
* Ember.js uses sometime the name **char** as a variable name and that isn't supported in rhino either. In each case the variable is renamed to **ch**

# License

This software is licensed under the Apache 2 license, quoted below.

Copyright 2013 by Carlos Quiroz

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

# Changelog

* version 1.3.0 Supports ember.js 1.3.0
* version 1.2.0 Supports ember.js 1.2.0 and adds smarter caching to compile only templates that have changed
* version 1.1.2 Supports ember.js 1.1.2 and play 2.2.x
* version 1.0.0 Supports ember.js 1.0.0
* version 0.6.0-SNAPSHOT Include support for ember.js 1.0 rc8
* version 0.5.0-SNAPSHOT Include support for ember.js 1.0 rc7
* version 0.4.0-SNAPSHOT Include support for ember.js 1.0 rc6
* version 0.3.0-SNAPSHOT Include support for ember.js 1.0 rc5
* version 0.2.0-SNAPSHOT Include support for ember.js 1.0 rc4
* version 0.1.0-SNAPSHOT Initial release
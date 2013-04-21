# Ember.js Plugin

This plugin provides build time compilation for [Ember.js](https://github.com/emberjs/ember.js) handlebar templates.

# How to install

* play 2.1.0:

```
addSbtPlugin("com.ketalo.play.plugins" % "emberjs" % "0.1.0-SNAPSHOT")
``` 

to your plugin.sbt

# How to Use

* Select your ember version in your Build.scala. Currently support versions include 1.0.0-rc.3, 1.0.0-rc.1 and 1.0.0-pre.2
```scala
import com.ketalo.EmberJsKeys
import sbt._

object ApplicationBuild extends Build with EmberJsKeys {

  val appName         = "play-emberjs-sample"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq.empty

  val main = play.Project(appName, appVersion, appDependencies).settings(
    emberJsVersion := "1.0.0-rc.3"
  )

}
```

* Include ember.js and the corresponding jQuery and handlebars files. Note that they are not provided by the sbt plugin. Check the ember site for them: [ember.js](https://ember.js) 
```html
    <script src="@routes.Assets.at("javascripts/jquery-1.8.2.min.js")" type="text/javascript"></script>
    <script src="@routes.Assets.at("javascripts/handlebars-1.0.rc.3.js")" type="text/javascript"></script>
    <script src="@routes.Assets.at("javascripts/ember-1.0.0-rc.3.js")" type="text/javascript"></script>
```

* Put your handlebar template (.handlebars) files under the ```app/assets/templates``` directory

* Reference the generated .js in a  ```<script>``` tag:
```
<script src="@routes.Assets.at("templates/template.pre.js")"></script>
```

The template.pre.js file generates a javascript containing all the templates in that directory

# Sample

For an example, see the bundled sample app for three different ember versions

* [ember rc.3](/sample-rc.3)
* [ember rc.1](/sample-rc.1)
* [ember pre.2](/sample-pre.2)

# Acknowledgments

This plugin was based the work from the blog post [Ember/Handlebars template precompilation with Play](http://eng.netwallet.com/2012/04/25/emberhandlebars-template-precompilation-with-play/)
A lot of the plugin internals were based on the [Dust.js play plugin](https://github.com/typesafehub/play-plugins/tree/master/dust)

## License

This software is licensed under the Apache 2 license, quoted below.

Copyright 2012 Typesafe (http://www.typesafe.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
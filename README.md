# CurseMaven [![Build Status](https://travis-ci.org/Wyn-Price/CurseMaven.svg?branch=master)](https://travis-ci.org/Wyn-Price/CurseMaven)
Gradle plugin to allow easy access to curseforge files, without using the curseforge maven   

# Applying the plugin
Using the plugins DSL:
```gradle
plugins {
  id "com.wynprice.cursemaven" version "1.1.0"
}
```
Using legacy plugin application:
```gradle
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "com.wynprice.cursemaven:CurseMaven:1.1.0"
  }
}

apply plugin: "com.wynprice.cursemaven"
```

# Simple usage
```gradle
dependencies {
  compile curse.resolve("jei", "2724420")
}
```
resolves the file [here](https://minecraft.curseforge.com/projects/jei/files/2724420), with the scope `compile`

```gradle
dependencies {
  deobfCompile curse.resolve("ctm", "2724420")
}
```
resolves the file [here](https://minecraft.curseforge.com/projects/ctm/files/2642375), with the scope `deobfCompile` 

# Custom Configuration
By default, the plugin will download any additional jars with the `source` classifier (`-sources.jar`).    
By default, the plugin will not do any debug logging.    
To change these properties, you need to define your own resolver.
```gradle
import com.wynprice.cursemaven.CurseMavenResolver

def myResolver = new CurseMavenResolver(attachSource: false, debug: true) //Don't attach sources, and allow debug 

dependencies {
  deobfCompile myResolver.resolve("ctm", "2724420")
}
```
The following would download ctm, without looking and downloading (if possible) the sources jar. It also enabled debugging. 

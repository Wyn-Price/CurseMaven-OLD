# [CurseMaven](https://login.gradle.org/plugin/com.wynprice.cursemaven) [![Build Status](https://travis-ci.org/Wyn-Price/CurseMaven.svg?branch=master)](https://travis-ci.org/Wyn-Price/CurseMaven)  
Gradle plugin to allow easy access to curseforge files, without using the curseforge maven.

This is documentation for versions 2.1.0 +
For Documentation on versions 1.x.x and 2.0.0, see [here](https://github.com/Wyn-Price/CurseMaven/blob/db9e2bf2daa9dc1cb8de883653c7cd63cb0b7e1f/README.md)

### Curse already has a maven, why does this exist?
I see alot of people saying how curseforge already has a maven and how there's no point in this plugin. While curseforge does indeed have a maven, some of the maven artifacts of the maven (artifact id, version id, classifier ect) all get derived from the jar file name. 

While this is good for if you have a jar properly named (which you should), there can be cases where the jar name is malformed. In these circumstances it can be very difficult to figure out what the maven artifacts should. 
Because each additional file you upload is also added to the curse maven depending on it's file name, you can end up in some situations where if the mod author has uploaded the sources along with their jar, the actual jar and the sources jar end up in different directories, meaning the sources won't be picked up. 

Additionally, if the mod you are trying to use has it's own maven, then you should use that. This is meant as a last resort when the only other option would be using jitpack (which is also an alternative you should consider) or downloading the file and putting it into `/libs/`, which won't work if somebody else clones the repository. 


# Applying the plugin
To see latest version, look [here](https://login.gradle.org/plugin/com.wynprice.cursemaven)
Using the plugins DSL:
```gradle
plugins {
  id "com.wynprice.cursemaven" version "x.x.x"
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
    classpath "com.wynprice.cursemaven:CurseMaven:x.x.x"
  }
}

apply plugin: "com.wynprice.cursemaven"
```
NOTE - the old way of doing `curse.resolve` and `curse.resolveID` will still work. (but not `curse.resolveURL`)
# Usage
Using the plugin is very simple. The dependency format is as follows:  
`curse.maven:<descriptor>:<fileid>`
 - `curse.maven` -> Required. Marks the dependency to be resolved by the curse maven plugin.
 - `<descriptor>` -> Can be anything you want. This file downloaded will have this in it's name, so it's good to use this to show which files are what. A good practice would be to have this as the project slug.
 - `<fileid>` -> the file id of the file you want to add as a dependency. 
```gradle
dependencies {
  compile "curse.maven:jei:2724420"
}
```
resolves the file [here](https://www.curseforge.com/minecraft/mc-mods/jei/files/2724420), with the scope `compile`

```gradle
dependencies {
  deobfCompile "curse.maven:ctm:2642375"
}
```
resolves the file [here](https://www.curseforge.com/minecraft/mc-mods/ctm/files/2642375), with the scope `deobfCompile`

# Special Thanks to 
 - [Tamaized](https://github.com/Tamaized) for working with me to figure out the cloudflare/403 issues.

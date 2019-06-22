# CurseMaven [![Build Status](https://travis-ci.org/Wyn-Price/CurseMaven.svg?branch=master)](https://travis-ci.org/Wyn-Price/CurseMaven)
Gradle plugin to allow easy access to curseforge files, without using the curseforge maven   

# Applying the plugin
Using the plugins DSL:
```gradle
plugins {
  id "com.wynprice.cursemaven" version "1.0.0"
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
    classpath "com.wynprice.cursemaven:CurseMaven:1.0.0"
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

# Dependencies
It is possible for CurseMaven to download curseforge dependencies. These dependencies will be taken from the `Related Projects > Required Dependency` section of the curseforge file page. ![img](https://imgur.com/PnYR993.png)   

Due to the way that ForgeGradle 2.3 works, to have curseforge dependencies you'll need to specify the dependency scope to use.    
Should you do this? no. Can you do this? yes.   
For example:   
```gradle
dependencies {
  deobfCompile myResolver.resolve("tinkers-construct", "2693496", "deobfCompile")
}
```
This would download the file [here](https://minecraft.curseforge.com/projects/tinkers-construct/files/26934960)    
as well as resolving the dependency `Mantle`.   
## Resolving Dependencies
Resolves dependencies from the curseforge file page.   
Let's use [this](https://minecraft.curseforge.com/projects/tinkers-construct/files/2693496) as an example. 
To resolve the dependencies, first CurseMaven would look at the file donwload page: 
![img](https://imgur.com/34zt9wf.png)   

CurseMaven first looks at at the `Supported Minecraft` section, and gets the minecraft versions of the main file.   
![img](https://imgur.com/AoH14s6.png)   

It then looks at the acceptable minecraft versions, (in this case 1.12.2), and stores it.   
We then look at the `Related Projects > Required Dependency` section of curseforge. ![img](https://imgur.com/PnYR993.png)   

For each dependency that is required, the following is done:   
 - CurseMaven goes onto the [dependency files page](https://minecraft.curseforge.com/projects/mantle/files)   
 - It then through each file on that page, checking the game versions and seeing if it matches the acceptable file versions from earlier. If there are no matches, then it goes onto [the next page](https://minecraft.curseforge.com/projects/mantle/files?page=2) and looks there. If it reaches the last file page, and no file has been found then the project is declared unsolvable.   
 - If CurseMaven finds the correct jar, it loads that jars file page, resolves the dependency with the given scope, and the process is repeated again. (In this cause, nothing would happen as Mantle doesn't have any dependencies, but if it did, then they would be loaded) 

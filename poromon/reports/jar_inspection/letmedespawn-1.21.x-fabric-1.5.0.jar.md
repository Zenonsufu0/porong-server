# letmedespawn-1.21.x-fabric-1.5.0.jar

## fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "letmedespawn",
  "version": "1.5.0",
  "name": "Let Me Despawn",
  "description": "Tweaks mob despawn rules to prevent accidental persistent mobs to increase performance.",
  "authors": [
    "frikinjay"
  ],
  "contact": {
    "homepage": "https://github.com/frikinjay/let-me-despawn"
  },
  "license": "LGPL-3.0",
  "icon": "assets/letmedespawn/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": [
      "com.frikinjay.letmedespawn.fabric.LetMeDespawnFabric"
    ],
    "client": []
  },
  "mixins": [
    "letmedespawn.mixins.json"
  ],
  "accessWidener": "letmedespawn.accesswidener",
  "depends": {
    "fabricloader": ">=0.16.7",
    "minecraft": "~1.21",
    "java": ">=21",
    "almanac": ">=1.0.2"
  },
  "suggests": {
    "another-mod": "*"
  }
}
```

## Namespaces

- assets: ['letmedespawn']

- data: []

## Lang files


## Interesting JSON/resources

- letmedespawn-1.21.x-common-common-refmap.json
- letmedespawn.mixins.json

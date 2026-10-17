# Problem Statement

## The scenario

An online store goes online with the settings it shipped with. Within minutes, automated scanners on the internet try its admin login, its debug paths, its static folders and its error pages.

## The naive version

Deploy the settings that worked on the developer's laptop, and change the ones someone remembers. Each forgotten setting is an open door, and the developer's comfort is the attacker's map.

## What this project must deliver

- A real store on loopback whose behaviour depends only on its settings.
- A checker that tests the live store from outside and its settings file from inside, with a score.
- A hardened production profile in which every setting is stated.
- A startup gate that refuses to start production with a default secret or a debugging switch.
- A drift check that compares production with development and the baseline.
- The same output on every run.

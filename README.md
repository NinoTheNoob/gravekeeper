# Gravekeeper

so i made this plugin because i kept dying and losing all my stuff before i could even get back to where i died lol. this fixes that.

## what it actually does

when you die, your inventory doesn't just spill all over the ground where anyone can grab it. instead it goes into a chest that spawns right where you died, with a little floating name tag above it so you know whose grave it is. only you (or admins) can open it.

if you don't come back in time the grave expires and dumps your stuff on the ground like normal, so don't take forever.

## commands

- `/grave` or `/grave list` - shows you all your open graves, where they are, and how much time is left before they expire
- `/grave tp <number>` - teleports you to one of your graves, use the number from the list
- `/gravekeeper reload` - reloads the config, need `gravekeeper.admin` perm for this

## how to use it

1. die (unfortunately)
2. a chest pops up where you died with your name floating above it
3. right click it like a normal chest and take your stuff back
4. once it's empty the chest and the name tag both disappear on their own
5. if you had xp levels saved, you get those back too once the grave is fully emptied

breaking the chest also works if you're the owner, it just drops everything like a normal chest would and removes the name tag.

## config

`config.yml` has the basics:

- `grave-lifetime-seconds` - how long before the grave expires (default 300 = 5 min)
- `on-expire` - what happens when it expires, `DROP` or `DELETE`
- `protect-from-others` - stops other players from touching your grave
- `allow-admin-access` - lets people with `gravekeeper.admin` open any grave

## building it

pretty standard, just run:

```
./gradlew build
```

jar shows up in `build/libs/`. needs java 21 and folia obviously since that's what this is built for.

## notes

made for folia 1.21.8 so all the scheduling stuff uses folia's region scheduler instead of the normal bukkit scheduler, otherwise things break in weird ways on multithreaded servers.

that's basically it, pretty simple plugin but does the job.

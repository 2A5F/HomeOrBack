# HomeOrBack
 Show a GUI interface after you die, letting you choose whether to respawn at spawn location or back to the location of death

Features:
- Auto respawn (optional)
- Auto back to death location (optional)
- Display GUI to choose respawn mode (optional)
- Back to the death location (optional)
- Back to the vicinity of the death location (optional)
- Show the location of death (optional)
- Try not in water (optional)
- Try not under leaves (optional)
- Use /back to back the death location (optional)
- Storge the death location (optional)
- Kill self command (optional)
- Support random teleport of cave world like world_nether
- Support for generating uniform ring random location

# Images
![Choose how to respawn](./images/1.png "Choose how to respawn")
![Respawn at the respawn location](./images/2.png "Respawn at the respawn location")
![Respawn near the death location](./images/3.png "Respawn near the death location")
![Commands](./images/4.png "Commands")
![Show death location](./images/5.png "Show death location")

# Config
```yaml
# Language; English: en, 简体普通话: cmn_Hans, 简体文言文: lzh_Hans
lang: en

# Will not display the death screen
auto_respawn: true
# Set to true to auto back to the place of death after the player dies
auto_back: false

# Back to the random ground near the place of death
back_random:
  enable: true
  # Must > 0
  min: 8
  # Must > min
  max: 32
  # The maximum number of try to generate a random location
  # Must >= 10
  max_try: 100
  # The number of times a player can retry after a failure
  # Must >= 1
  max_retry: 3
  # Generate uniformly random location
  uniform: true
  # Generate random location on land as much as possible
  try_to_land: true
  # Never generate random location in the water
  never_water: false
  # Try generate under the tree
  try_not_leaves: true

# Use /back to back at any time
back_command: true
# show 'you can use /back' message
# require {back_command:true}
show_back_command_msg: false
# Will not display the respawn GUI
# require {back_command: true}
no_gui: false

# Show death location
show_death_loc: false

# Store the location of the death
store_location: true

# Use /killself to kill self
kill_self_command: true
```
- If you want to add a cave world like world_nether, just add or modify the "world_name: true" in worlds.yml.
However, the default will automatically judge, based on whether the top layer has bedrock and random sampling



# EzGUI-MOD

A Fabric-based Minecraft inventory-menu project. This fork's original README
credited **tickwarden**; the mod metadata names **MesterMan03** as an author,
and the GitHub fork identifies [MesterMan03/EzGUI](https://github.com/MesterMan03/EzGUI)
as its upstream. The source implements JSON-defined menus, an in-game editor,
and buttons that can open pages, close the menu, run commands or toggle state.

> **Status:** This README describes the checked-in source, not a successful
> build or a tested release. Review the limitations and licensing notes below
> before installing or redistributing it.

## Requirements and version information

| Setting | Value in this checkout |
| --- | --- |
| Minecraft | 1.21.1 (`~1.21.1` in Fabric metadata) |
| Java | 21 or later according to Fabric metadata; build targets Java 21 |
| Fabric Loader | `0.16.5` or later according to Fabric metadata |
| Fabric API | Required; the build uses `0.102.0+1.21.1` |
| Build tooling | Fabric Loom `1.8.11`; Gradle wrapper distribution `8.10` |
| Project/mod version | `1.0.0` in `gradle.properties` |

These values are configuration evidence, not a promise that other versions work.
Read [`gradle.properties`](gradle.properties), [`build.gradle`](build.gradle)
and [`fabric.mod.json`](src/main/resources/fabric.mod.json) for the current values.
The mod uses Minecraft's handled inventory screens and registers its commands
through Fabric's command API.

## Build and installation overview

The repository includes `gradlew` and the Gradle wrapper; no prebuilt artifact
is assumed. In an environment you trust, with Java 21 and network access for
the wrapper and dependencies, the conventional build command is:

```sh
bash ./gradlew build
```

Inspect the build scripts and wrapper before running them. A successful build
would place Gradle outputs under `build/`; verify the resulting remapped JAR
and its contents rather than assuming every output is suitable for a server.
Install the verified mod together with the compatible Fabric Loader and Fabric
API, following their own installation guidance. Back up the server/world and
configuration first. This documentation change did **not** run the build or a
Minecraft server.

## Configuration and menu data

`GuiManager` looks for JSON files in Fabric Loader's config directory under
`ezgui/` (normally `config/ezgui/`). Each file's base name is used as its menu
ID. The source creates an example `default_menu.json` only when it creates the
configuration directory; an already-existing empty directory does not trigger
that example. Use `/ezgui reload` after changing files, and inspect the logs if
JSON cannot be loaded.

The following is a minimal illustration of the fields used by the source; it
has not been tested in a running game:

```json
{
  "title": "&6Example menu",
  "rows": 3,
  "buttons": [
    {
      "slot": 13,
      "item": {"id": "minecraft:diamond", "name": "&bHello"},
      "action": {"type": "command", "command": "say Hello %player%!", "as_server": false}
    },
    {
      "slot": 26,
      "item": {"id": "minecraft:barrier", "name": "&cClose"},
      "action": {"type": "close"}
    }
  ]
}
```

A menu has one to six rows when created through the command; a row contains
nine slots, numbered from zero. The source replaces `&` with the section-sign
formatting character in titles and item names. Invalid or unrecognized item
identifiers, manually edited row counts and malformed JSON need independent
validation; the code does not provide a complete schema validator.

### Button and action fields

- `slot` selects an inventory position; `item.id` is an item identifier and
  `item.name` is an optional custom name.
- `action.type` may be `close`, `open_page`, `command` or `toggle`.
  `open_page` uses `page` as another menu ID. `command` uses `command` and
  `as_server`; the string `%player%` is replaced with the clicking player's
  name. A toggle may use `command`, `command_off` and `toggled_item`.
- `required_permission` and `required_xp` gate an action. The implementation
  deducts the specified number of **experience levels** after those checks;
  `sound` and `deny_message` are optional. Validate the behavior in a safe test
  world before relying on it.
- `is_holder` permits normal slot handling for that button instead of treating
  it as a protected action slot. Shift-click quick-move returns an empty stack
  in this screen handler.
- The data class declares item `lore` and `custom_model_data`, but the current
  item-creation method applies only the ID and custom name. Do not assume the
  other two fields are rendered by this checkout.

Toggle state is kept in the screen-handler instance; this source does not show
persistent toggle-state storage. The generated example refers to `second_page`,
which must exist separately for that page action to find it.

## Commands

The `/ezgui` command tree requires permission level 2. `open` and `edit` need a
player command source. IDs are single-word command arguments. Assigning an
action requires an existing button in the target slot: add an item in the
editor first and close the editor to save it.

| Command | Purpose |
| --- | --- |
| `/ezgui reload` | Reload JSON definitions from the config directory. |
| `/ezgui create <id> <rows> <title>` | Create an empty menu; `rows` is 1–6. |
| `/ezgui edit <id>` | Open the item editor for an existing menu. |
| `/ezgui open <id>` | Open an existing menu for the command's player. |
| `/ezgui action <id> <slot> close` | Assign a close action. |
| `/ezgui action <id> <slot> clear` | Clear that button's action. |
| `/ezgui action <id> <slot> page <page_id>` | Open another menu when the button is used. |
| `/ezgui action <id> <slot> command <as_server> <command>` | Assign a command; `as_server` is `true` or `false`. |
| `/ezgui action <id> <slot> set_sound <sound_id>` | Set the action's sound identifier. |
| `/ezgui action <id> <slot> set_xp_cost <levels>` | Set the non-negative experience-level cost. |
| `/ezgui action <id> <slot> set_permission <level>` | Set the required permission level, 0–4. |
| `/ezgui action <id> <slot> set_holder <true_or_false>` | Set whether the button slot is a holder. |

The command parser accepts slots `0`–`53`; a menu with fewer than six rows has
fewer usable slots. `set_sound`, `set_xp_cost` and `set_permission` create an
empty action object when necessary, but an action still needs a usable `type`.
The editor saves the displayed item IDs and custom names when it closes and
keeps prior actions/holder flags for matching slots.

## Security and operational cautions

A configured action with `as_server: true` is executed with the server command
source; this can have far more authority than an ordinary player. Treat JSON
files and in-game editing as privileged configuration, accept definitions only
from trusted people, and review every command before loading it. The default
per-action permission is zero unless configured. This README does not claim the
mod is hardened or safe for untrusted menu definitions.

The tracked tree has no `LICENSE` file even though `fabric.mod.json` says
`MIT` and `build.gradle` attempts to include `LICENSE` in the JAR. The metadata
also names an icon path not present in the tracked tree. Resolve those
inconsistencies with the maintainers/upstream before treating the build or
redistribution terms as verified. No license text is invented here.

## Source map

- [`EzGUIMod.java`](src/main/java/com/mesterman03/ezgui_fabric/EzGUIMod.java): initialization and command registration.
- [`GuiManager.java`](src/main/java/com/mesterman03/ezgui_fabric/GuiManager.java): JSON loading, saving and example creation.
- [`EzGUICommand.java`](src/main/java/com/mesterman03/ezgui_fabric/EzGUICommand.java): command parsing and permissions.
- [`EditorScreenHandler.java`](src/main/java/com/mesterman03/ezgui_fabric/EditorScreenHandler.java): in-game item editor.
- [`EzGUIScreenHandler.java`](src/main/java/com/mesterman03/ezgui_fabric/EzGUIScreenHandler.java): display, click handling and actions.
- `GuiDefinition`, `GuiButton`, `GuiItem` and `GuiAction` define the JSON-backed fields in the same Java package.

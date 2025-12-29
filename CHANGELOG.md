## BlockRegen v1.2 Changelog

-   **Fixed ItemsAdder Custom Drops:** Resolved an issue where custom drops using ItemsAdder items were not functioning correctly. The plugin now properly recognizes and handles ItemsAdder custom item IDs, including those with custom prefixes like `noblemetals:`.
-   **Improved Update Notification System:** Implemented a configurable update notification message. Server administrators can now customize the update availability message via the `config.yml` file.
-   **Confirmed MMOItems Custom Drop Compatibility:** Verified and ensured full compatibility with MMOItems for custom block drops. Players can now reliably receive MMOItems as custom drops from regenerated blocks, provided the correct `mmoitems:TYPE:ID` format is used in `blocks.yml`.
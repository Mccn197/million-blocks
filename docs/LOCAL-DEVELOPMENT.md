# Local development workflow

## Requirements
- Java 25 (Paper 26.1+ requires Java 25)
- Maven 3.9+
- 4 GB RAM recommended for a local Paper server

## Setup
1. Run `scripts/setup-local.sh` (Linux/macOS) or `scripts/setup-local.ps1` (Windows PowerShell).
2. Build with `mvn clean package`.
3. Copy `target/MillionBlocks-*.jar` into `local/server/plugins/`.
4. Start with the generated script in `local/server/`.
5. Accept the EULA on first setup (`eula=true` is written by our setup scripts).

The setup script downloads the latest STABLE Paper build for Minecraft 26.2 using PaperMC's official Downloads Service.

## Fast test loop
- `/mb arena reset`
- `/mb progress set 324999`
- `/mb progress add 1`
- `/mb give laser`
- test the laser
- `/mb arena reset`

For victory:
- `/mb progress set 999999`
- break one block, or `/mb victory`

For all tools:
- `/mb give all`

## Map strategy
Keep the filming world separate from logic. The plugin should work against any survival map. For destructive integration tests, use a disposable test arena or a copied world folder.

# Toolbox MCP (powered with Quarkus)

To use it:
* build it and make it resolvable (meaning `mvn install`)
* add following snippet to MCP client (ie Claude Desktop uses `$HOME/.config/Claude/claude_desktop_config.json`):
```json
    "mcpServers": {
        "toolbox": {
            "command": "jbang",
            "args": ["--quiet",
                    "eu.maveniverse.maven.toolbox:mcp:0.14.6-SNAPSHOT:runner"]
        }
    }
```

# Schema source

Pin: `f391a1fa`.

- `Packages/ChronoFlowKit/Sources/LocalStore/SchemaV1.swift`, lines 7–38.
- `Packages/ChronoFlowKit/Tests/LocalStoreTests/Fixtures/rn-schema-v1.json` (byte copy).

```sh
git -C /Users/rickgout/Sites/chronoflow-ios show f391a1fa:Packages/ChronoFlowKit/Sources/LocalStore/SchemaV1.swift \
  | sed -n '7,38p' | sed 's/^                //' > core/database/src/test/resources/ios-schema-v1.sql
```

SHA-256:

- `ios-schema-v1.sql`: `6014a032b63a97f72d24858d4c08f1fcb496bb0a44dcd70af25b70688ee4b583`
- `rn-schema-v1.json`: `937c93693e8dc17c60572404a6cb0825860bb6aafb9840b3bde0ee9b2e8dd54a`

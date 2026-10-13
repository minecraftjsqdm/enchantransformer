# 附魔转移台 / Enchantment Transfuser

NeoForge 1.21.1 · MIT · mod id `enchantransformer` · v1.0.1

## English

Adds one block — the **Enchantment Transfuser** — that moves enchantments from one item to another, one enchantment at a time.

- Put a donor item in the input slot and a target in the receiver slot, then click **Transfer to A** / **Transfer to B** to move a single enchantment.
- **Cost** — experience levels per transfer: 4 for a normal enchantment, 6 for treasure, 7 for a curse.
- **Checks** — it refuses to transfer when the input or receiver slot is empty, nothing is left to transfer, you lack the levels, the receiver already has the enchantment at the same or a higher level, the receiver cannot take enchantments, or the enchantment conflicts with one already on the receiver. Each case shows its own reason message.

## 中文

新增一个方块——**附魔转移台**——把附魔从一件物品转移到另一件，每次只转移一条。

- 在输入槽放来源物品、接收槽放目标物品，点击 **转移到 A** / **转移到 B** 即可转移单条附魔。
- 消耗：每转移一条消耗经验等级——普通附魔 4 级、宝藏附魔 6 级、诅咒 7 级。
- 校验：当输入 / 接收槽为空、已无可转移附魔、等级不足、接收方已有同级或更高等级的该附魔、接收方无法附魔、或与接收方已有附魔冲突时，会拒绝转移，并给出对应的原因提示。

## Dependencies / 依赖

- Required / 必需：NeoForge 21.1.x on Minecraft 1.21.1

## Build / 构建

```sh
# Windows
$env:JAVA_HOME='<path-to-jdk21>'; .\gradlew.bat build --console=plain -q

# Linux / macOS
JAVA_HOME=<path-to-jdk21> ./gradlew build
```

Output jar: `build/libs/enchantransformer-1.0.1.jar`. 产物：`build/libs/enchantransformer-1.0.1.jar`。

## License / 许可

MIT. See `LICENSE`.

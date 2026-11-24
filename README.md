# Heads Crafting

Paper 26.3 插件：玩家先合成「空白頭顱」，再用一本書把它變成玩家頭或裝飾頭。
離線模式（`online-mode=false`）的伺服器也能顯示正確皮膚。

## 玩法

1. 合成空白頭顱（配方在 `config.yml` 的 `recipe`，目前預設關閉）。
2. 準備一本書：
   - **玩家頭：** 書與筆寫好後簽名，**標題**寫玩家名。
   - **裝飾頭：** 把 [minecraft-heads.com](https://minecraft-heads.com) 頁面「For Developers」的 value 貼進書與筆的**第一頁**，不用簽名。
3. 一手拿空白頭顱、另一手拿書（主手副手都可以），按 **F**（換手）。
4. 那一疊空白頭顱變成同一個頭，一次最多 64 顆，每次消耗一本書。

## 皮膚從哪裡來

依序嘗試，第一個找到的就用：

1. 插件自己的快取 `plugins/HeadsCrafting/cache.json`（預設 7 天）
2. Mojang：用名字查正版 UUID，再取材質與簽名。不讀伺服器的 usercache，所以不會拿到離線 UUID。
3. SkinsRestorer（有安裝才會用）：先找玩家用 `/skin` 設定的皮膚，再找它存的皮膚。

都找不到時不轉換，空白頭顱和書都會留著。

## 權限

| 權限 | 預設 | 用途 |
| --- | --- | --- |
| `headscrafting.craft` | 所有人 | 合成空白頭顱 |
| `headscrafting.player` | 所有人 | 做自己的頭 |
| `headscrafting.player.others` | 所有人 | 做別人的頭 |
| `headscrafting.custom` | 所有人 | 做裝飾頭 |
| `headscrafting.admin` | OP | `/headscrafting reload`、`/headscrafting giveblank [數量]` |

## 建置

需要 JDK 25。

```
./gradlew build
```

產出 `build/libs/HeadsCrafting-<版本>.jar`。

沒有網路時，可以改用本機的 paper-api 與 SkinsRestorer jar 編譯：

```
./gradlew build -PlocalLibs=<放 jar 的資料夾>
```

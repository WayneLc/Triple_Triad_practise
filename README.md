# 九宮幻卡 MVP

Java 25 / Spring Boot 3 的九宮幻卡遊戲 MVP。前端使用原生 HTML、CSS、JavaScript，由 Spring Boot 的 `static` 目錄直接提供，資料庫使用 MySQL 8。

## 在 VS Code 執行

1. 在 MySQL Workbench 開啟並執行 `database/mysql-workbench-init.sql`。
2. 設定環境變數：`DB_USERNAME` 與 `DB_PASSWORD`；或在 `application.properties` 填入 MySQL 帳密。
3. 安裝 JDK 25 與 Maven 3.9+。
4. 以 VS Code 開啟本專案資料夾，建議安裝 Extension Pack for Java 與 Spring Boot Extension Pack。
5. 在終端執行 `mvn spring-boot:run`，再開啟 `http://localhost:8080`。

資料庫 schema 為 `triple_triad`，可由 MySQL Workbench 管理。設計恰好使用五張資料表：

- `players`：帳號、密碼、首次教學狀態
- `cards`：可擴充的卡牌資料；首次啟動自動建立 40 張 1 至 5 星卡
- `decks`：每位玩家最多三組卡組
- `rooms`：最多十間等候房間，五分鐘未加入自動移除
- `battles`：預留對戰紀錄與棋盤狀態

## MVP 行為

- 卡組為五張不同卡牌：最多 1 張五星與 1 張四星，或 2 張四星；其餘三張為 1 至 3 星。
- 連線房間可以設定 Standard、Same、Plus、Random、4 位數密碼與 30 至 60 秒回合時間。
- 連線房間五分鐘無人加入會移除。房主登出或離開頁面時，其等候房間會移除。
- 對戰棋盤為 3 x 3；標準翻牌邏輯已可遊玩，Same/Plus/Random 目前作為規則選擇與未來擴充點。
- 電腦對戰目前提供「簡單」難度，AI 會隨機出牌。

密碼目前為 MVP 的明文儲存。部署前應改用 BCrypt、登入 session/JWT、WebSocket 即時房間同步，以及伺服器端斷線偵測。

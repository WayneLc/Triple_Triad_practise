package com.tripletriad;

import com.tripletriad.model.*;
import com.tripletriad.repo.*;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class GameController {
    private final PlayerRepository players; private final CardRepository cards; private final DeckRepository decks;
    private final RoomRepository rooms;
    public GameController(PlayerRepository players, CardRepository cards, DeckRepository decks, RoomRepository rooms) {
        this.players=players; this.cards=cards; this.decks=decks; this.rooms=rooms;
    }

    @PostConstruct void seedCards() {
        if (cards.count() > 0) return;
        String[] names = {"沙漠鼠","陸行鳥","莫古力","仙人掌","炸彈怪","哥布林","狼蛛","樹精","海豚","陸蟹","貝希摩斯幼體","雪人","鯨頭鶴","獅鷲","蠍尾獅","魔界花","石兵","水蛇","火龍","魔導兵器","伊弗利特","迦樓羅","泰坦","利維坦","奧丁","巴哈姆特","希瓦","拉姆","亞歷山大","菲尼克斯","尼德霍格","佐迪亞克","海德林","賢王莫古","月讀命","神龍","絕槍戰士","暗影使者","光之戰士","九宮之王"};
        for (int i=0; i<40; i++) { int star=i/8+1; int a=1+(i*3)%9, b=1+(i*5)%9, c=1+(i*7)%9, d=1+(i*2)%9; cards.save(new Card(names[i],star,a,b,c,d, new String[]{"火","水","風","土","無"}[i%5])); }
    }

    @PostMapping("/auth/register") public Map<String,Object> register(@RequestBody AuthRequest r) {
        if (blank(r.username()) || blank(r.password())) throw bad("請輸入玩家名稱與密碼");
        if (players.findByUsername(r.username().trim()).isPresent()) throw bad("此玩家名稱已被使用");
        Player p=players.save(new Player(r.username().trim(), r.password())); return playerResponse(p, true);
    }
    @PostMapping("/auth/login") public Map<String,Object> login(@RequestBody AuthRequest r) {
        Player p=players.findByUsername(r.username()).orElseThrow(()->bad("帳號或密碼錯誤"));
        if (!p.password.equals(r.password())) throw bad("帳號或密碼錯誤"); return playerResponse(p, !p.tutorialSeen);
    }
    @PostMapping("/players/{id}/tutorial-complete") public void tutorialDone(@PathVariable Long id) { Player p=getPlayer(id); p.tutorialSeen=true; players.save(p); }
    @GetMapping("/cards") public List<Card> allCards() { return cards.findAll(); }
    @GetMapping("/players/{id}/decks") public List<Deck> playerDecks(@PathVariable Long id) { return decks.findByPlayerId(id); }
    @PostMapping("/players/{id}/decks") public Deck makeDeck(@PathVariable Long id, @RequestBody DeckRequest r) {
        List<Deck> current=decks.findByPlayerId(id); if(current.size()>=3) throw bad("每位玩家最多建立 3 個卡組");
        validateDeck(r.cardIds()); Deck d=new Deck(); d.player=getPlayer(id); d.name=blank(r.name())?"我的卡組 "+(current.size()+1):r.name(); d.cardIds=String.join(",",r.cardIds().stream().map(String::valueOf).toList()); return decks.save(d);
    }
    @DeleteMapping("/decks/{deckId}") public void deleteDeck(@PathVariable Long deckId) { decks.deleteById(deckId); }

    @GetMapping("/rooms") public List<Room> listRooms() { cleanupRooms(); return rooms.findAll(); }
    @PostMapping("/rooms") public Room createRoom(@RequestBody RoomRequest r) {
        cleanupRooms(); if(rooms.count()>=10) throw bad("目前房間已滿（最多 10 間）");
        if(r.turnSeconds()<30 || r.turnSeconds()>60) throw bad("回合時間必須在 30 至 60 秒之間");
        if(!blank(r.password()) && !r.password().matches("\\d{4}")) throw bad("房間密碼必須為 4 位數字");
        Room room=new Room(); room.host=getPlayer(r.hostId()); room.title=blank(r.title())?"等待挑戰者":r.title(); room.ruleName=blank(r.ruleName())?"STANDARD":r.ruleName(); room.password=r.password(); room.turnSeconds=r.turnSeconds(); return rooms.save(room);
    }
    @PostMapping("/rooms/{id}/join") public Map<String,Object> joinRoom(@PathVariable Long id, @RequestBody JoinRequest r) {
        Room room=rooms.findById(id).orElseThrow(()->bad("房間已不存在")); if(!Objects.equals(room.password, blank(r.password())?null:r.password())) throw bad("房間密碼錯誤");
        rooms.delete(room); return Map.of("battleId", "online-"+UUID.randomUUID(), "ruleName",room.ruleName,"turnSeconds",room.turnSeconds,"opponent",room.host.username);
    }
    @DeleteMapping("/rooms/{roomId}/host/{hostId}") public void closeRoom(@PathVariable Long roomId, @PathVariable Long hostId) {
        Room room=rooms.findById(roomId).orElseThrow(()->bad("房間已不存在"));
        if (!room.host.id.equals(hostId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有房主可以解散房間");
        rooms.delete(room);
    }
    @PostMapping("/battle/ai") public Map<String,Object> aiBattle(@RequestBody AiRequest r) { return Map.of("battleId","ai-"+UUID.randomUUID(),"ruleName",r.ruleName(),"turnSeconds",60,"opponent","簡單 AI"); }
    @DeleteMapping("/rooms/host/{playerId}") public void closeHostRooms(@PathVariable Long playerId) { rooms.findAll().stream().filter(x->x.host.id.equals(playerId)).forEach(rooms::delete); }

    private void cleanupRooms() { rooms.findAll().stream().filter(r->Duration.between(r.createdAt,Instant.now()).toMinutes()>=5).forEach(rooms::delete); }
    private void validateDeck(List<Long> ids) { if(ids==null || ids.size()!=5 || new HashSet<>(ids).size()!=5) throw bad("卡組必須剛好包含 5 張不同卡牌"); List<Card> selected=cards.findAllById(ids); if(selected.size()!=5) throw bad("找不到指定卡牌"); long five=selected.stream().filter(c->c.stars==5).count(), four=selected.stream().filter(c->c.stars==4).count(); if(five>1 || four>2 || five+four>2) throw bad("最多 1 張五星與 1 張四星，或最多 2 張四星"); if(selected.stream().filter(c->c.stars<=3).count()<3) throw bad("其餘 3 張必須為 1 至 3 星卡牌"); }
    private Player getPlayer(Long id) { return players.findById(id).orElseThrow(()->bad("找不到玩家")); }
    private Map<String,Object> playerResponse(Player p, boolean showTutorial) { return Map.of("id",p.id,"username",p.username,"showTutorial",showTutorial); }
    private boolean blank(String s) { return s==null || s.isBlank(); }
    private ResponseStatusException bad(String msg) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,msg); }
    public record AuthRequest(String username,String password) {} public record DeckRequest(String name,List<Long> cardIds) {} public record RoomRequest(Long hostId,String title,String ruleName,String password,int turnSeconds) {} public record JoinRequest(String password) {} public record AiRequest(String ruleName) {}
}

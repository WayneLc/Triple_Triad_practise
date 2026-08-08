package com.tripletriad;

import com.tripletriad.model.*;
import com.tripletriad.repo.*;
import jakarta.annotation.PostConstruct;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@SuppressWarnings("null")
@RestController @RequestMapping("/api")
public class GameController {
  private final PlayerRepository players; private final CardRepository cards; private final DeckRepository decks; private final RoomRepository rooms;
  public GameController(PlayerRepository p,CardRepository c,DeckRepository d,RoomRepository r){players=p;cards=c;decks=d;rooms=r;}
  @PostConstruct void seed(){if(cards.count()>0)return; for(int i=0;i<40;i++)cards.save(new Card("卡牌 "+(i+1),i/8+1,1+i*3%9,1+i*5%9,1+i*7%9,1+i*2%9,new String[]{"火","冰","雷","風","土"}[i%5]));}
  @PostMapping("/auth/register") public Map<String,Object> register(@RequestBody Auth x){if(blank(x.username())||blank(x.password()))throw bad("帳號與密碼不可空白");if(players.findByUsername(x.username().trim()).isPresent())throw bad("帳號已存在");return player(players.save(new Player(x.username().trim(),x.password())),true);}
  @PostMapping("/auth/login") public Map<String,Object> login(@RequestBody Auth x){Player p=players.findByUsername(x.username()).orElseThrow(()->bad("帳號或密碼錯誤"));if(!p.password.equals(x.password()))throw bad("帳號或密碼錯誤");return player(p,!p.tutorialSeen);}
  @PostMapping("/players/{id}/tutorial-complete") public void tutorial(@PathVariable Long id){Player p=getPlayer(id);p.tutorialSeen=true;players.save(p);}
  @GetMapping("/cards") public List<Card> cards(){return cards.findAll();}
  @GetMapping("/players/{id}/decks") public List<Deck> decks(@PathVariable Long id){return decks.findByPlayerId(id);}
  @PostMapping("/players/{id}/decks") public Deck deck(@PathVariable Long id,@RequestBody DeckReq x){List<Deck> all=decks.findByPlayerId(id);if(all.size()>=3)throw bad("最多儲存 3 組卡組");validate(x.cardIds());Deck d=new Deck();d.player=getPlayer(id);d.name=blank(x.name())?"卡組 "+(all.size()+1):x.name();d.cardIds=String.join(",",x.cardIds().stream().map(String::valueOf).toList());return decks.save(d);}
  @DeleteMapping("/decks/{id}") public void deleteDeck(@PathVariable Long id){decks.deleteById(Objects.requireNonNull(id,"deck id"));}

  @GetMapping("/rooms") public List<Map<String,Object>> list(){clean();return rooms.findAll().stream().map(this::view).toList();}
  @GetMapping("/rooms/{id}") public Map<String,Object> room(@PathVariable Long id){clean();return view(getRoom(id));}
  @PostMapping("/rooms") public Map<String,Object> create(@RequestBody RoomReq x){clean();if(rooms.count()>=10)throw bad("大廳最多 10 個房間");if(x.turnSeconds()<30||x.turnSeconds()>60)throw bad("思考時間需介於 30 到 60 秒");Room r=new Room();r.host=getPlayer(x.hostId());r.title=blank(x.title())?r.host.username+" 的房間":x.title();r.ruleName=blank(x.ruleName())?"STANDARD":x.ruleName();r.password=blank(x.password())?null:x.password();r.turnSeconds=x.turnSeconds();return view(rooms.save(r));}
  @PostMapping("/rooms/{id}/join") public Map<String,Object> join(@PathVariable Long id,@RequestBody JoinReq x){Room r=getRoom(id);if(!Objects.equals(r.password,blank(x.password())?null:x.password()))throw bad("房間密碼錯誤");if(r.status.equals("PLAYING"))throw bad("此房間遊玩中");if(r.guest!=null&&!r.guest.id.equals(x.playerId()))throw bad("此房間人數已滿");if(!r.host.id.equals(x.playerId())){r.guest=getPlayer(x.playerId());r.guestDeckId=x.deckId();r.guestReady=false;r.expiresAt=Instant.now().plusSeconds(240);rooms.save(r);}return view(r);}
  @PostMapping("/rooms/{id}/deck") public Map<String,Object> selectDeck(@PathVariable Long id,@RequestBody DeckChoice x){Room r=getRoom(id);if(r.host.id.equals(x.playerId())){if(r.hostReady)throw bad("請先取消準備");r.hostDeckId=x.deckId();}else{guest(r,x.playerId());if(r.guestReady)throw bad("請先取消準備");r.guestDeckId=x.deckId();}rooms.save(r);return view(r);}
  @PostMapping("/rooms/{id}/ready") public Map<String,Object> ready(@PathVariable Long id,@RequestBody Action x){Room r=getRoom(id);if(r.host.id.equals(x.playerId())){if(blank(r.hostDeckId))throw bad("請先選擇卡組");r.hostReady=x.value();}else{guest(r,x.playerId());if(blank(r.guestDeckId))throw bad("請先選擇卡組");r.guestReady=x.value();}rooms.save(r);return view(r);}
  @PostMapping("/rooms/{id}/start") public Map<String,Object> start(@PathVariable Long id,@RequestBody Action x){Room r=getRoom(id);if(!r.host.id.equals(x.playerId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"只有房主可開始");if(r.guest==null||!r.hostReady||!r.guestReady)throw bad("雙方都必須準備完畢");r.status="PLAYING";rooms.save(r);return view(r);}
  @PostMapping("/rooms/{id}/leave") public Map<String,Object> leave(@PathVariable Long id,@RequestBody Action x){Room r=getRoom(id);if(r.host.id.equals(x.playerId())){rooms.delete(r);return Map.of("disbanded",true);}guest(r,x.playerId());r.guest=null;r.guestDeckId=null;r.guestReady=false;r.hostReady=false;r.status="WAITING";r.expiresAt=Instant.now().plusSeconds(240);rooms.save(r);return view(r);}
  @PostMapping("/rooms/{id}/state") public Map<String,Object> state(@PathVariable Long id,@RequestBody State x){Room r=getRoom(id);if(!r.host.id.equals(x.playerId())&&(r.guest==null||!r.guest.id.equals(x.playerId())))throw new ResponseStatusException(HttpStatus.FORBIDDEN);r.gameState=x.state();rooms.save(r);return view(r);}
  @DeleteMapping("/rooms/{id}/host/{player}") public void close(@PathVariable Long id,@PathVariable Long player){Room r=getRoom(id);if(!r.host.id.equals(player))throw new ResponseStatusException(HttpStatus.FORBIDDEN);rooms.delete(r);}
  @DeleteMapping("/rooms/host/{id}") public void closeHost(@PathVariable Long id){rooms.findAll().stream().filter(r->r.host.id.equals(id)).forEach(rooms::delete);}
  @PostMapping("/battle/ai") public Map<String,Object> ai(@RequestBody Ai x){return Map.of("battleId","ai-"+UUID.randomUUID(),"ruleName",x.ruleName(),"turnSeconds",60,"opponent","電腦 AI");}
  private void clean(){Instant n=Instant.now();rooms.findAll().stream().filter(r->r.guest==null&&r.expiresAt!=null&&r.expiresAt.isBefore(n)).forEach(rooms::delete);}
  private Map<String,Object> view(Room r){Map<String,Object>m=new LinkedHashMap<>();m.put("id",r.id);m.put("title",r.title);m.put("ruleName",r.ruleName);m.put("turnSeconds",r.turnSeconds);m.put("hostId",r.host.id);m.put("hostName",r.host.username);m.put("guestId",r.guest==null?null:r.guest.id);m.put("guestName",r.guest==null?null:r.guest.username);m.put("hostDeckId",r.hostDeckId==null?"":r.hostDeckId);m.put("guestDeckId",r.guestDeckId==null?"":r.guestDeckId);m.put("hostReady",r.hostReady);m.put("guestReady",r.guestReady);m.put("status",r.status);m.put("passwordRequired",!blank(r.password));m.put("expiresAt",r.expiresAt.toString());m.put("gameState",r.gameState==null?"":r.gameState);return m;}
  private void validate(List<Long> x){if(x==null||x.size()!=5||new HashSet<>(x).size()!=5||cards.findAllById(x).size()!=5)throw bad("卡組必須由 5 張不同的卡組成");}
  private Player getPlayer(Long id){return players.findById(id).orElseThrow(()->bad("找不到玩家"));} private Room getRoom(Long id){return rooms.findById(id).orElseThrow(()->bad("找不到房間"));} private void guest(Room r,Long p){if(r.guest==null||!r.guest.id.equals(p))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"你不在此房間");} private Map<String,Object> player(Player p,boolean t){return Map.of("id",p.id,"username",p.username,"showTutorial",t);} private boolean blank(String s){return s==null||s.isBlank();} private ResponseStatusException bad(String s){return new ResponseStatusException(HttpStatus.BAD_REQUEST,s);}
  record Auth(String username,String password){} record DeckReq(String name,List<Long> cardIds){} record RoomReq(Long hostId,String title,String ruleName,String password,int turnSeconds){} record JoinReq(String password,Long playerId,String deckId){} record DeckChoice(Long playerId,String deckId){} record Action(Long playerId,boolean value){} record State(Long playerId,String state){} record Ai(String ruleName){}
}

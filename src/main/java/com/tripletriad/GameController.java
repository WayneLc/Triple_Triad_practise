package com.tripletriad;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.tripletriad.model.*;
import com.tripletriad.repo.*;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings("null") // Spring validates required @PathVariable values before controller invocation.
@RestController @RequestMapping("/api")
public class GameController {
  private static final Pattern USERNAME=Pattern.compile("[\\p{L}\\p{N}_-]{3,32}");
  private final PlayerRepository players; private final CardRepository cards; private final DeckRepository decks; private final RoomRepository rooms; private final ObjectMapper json;
  private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
  public GameController(PlayerRepository p,CardRepository c,DeckRepository d,RoomRepository r,ObjectMapper j){players=p;cards=c;decks=d;rooms=r;json=j;}
  @PostConstruct void seed(){if(cards.count()>0)return;for(int i=0;i<40;i++)cards.save(new Card("Card "+(i+1),i/8+1,1+i*3%9,1+i*5%9,1+i*7%9,1+i*2%9,"NONE"));}

  @PostMapping("/auth/register") public Map<String,Object> register(@RequestBody Auth x){String u=username(x.username());if(x.password()==null||x.password().length()<8||x.password().length()>72)throw bad("密碼必須為 8 至 72 個字元。");if(players.findByUsername(u).isPresent())throw bad("此帳號已被使用。");return player(players.save(new Player(u,encoder.encode(x.password()))),true);}
  @PostMapping("/auth/login") public Map<String,Object> login(@RequestBody Auth x){String u=username(x.username());Player p=players.findByUsername(u).orElseThrow(()->bad("帳號或密碼錯誤。"));if(!matches(x.password(),p.password))throw bad("帳號或密碼錯誤。");if(!p.password.startsWith("$2")){p.password=encoder.encode(x.password());players.save(p);}return player(p,!p.tutorialSeen);}
  @PostMapping("/players/{id}/tutorial-complete") public void tutorial(@PathVariable Long id){Player p=player(id);p.tutorialSeen=true;players.save(p);}
  @GetMapping("/cards") public List<Card> cards(){return cards.findAll();}
  @GetMapping("/players/{id}/decks") public List<Deck> decks(@PathVariable Long id){return decks.findByPlayerId(id);}
  @PostMapping("/players/{id}/decks") public Deck deck(@PathVariable Long id,@RequestBody DeckRequest x){List<Deck> a=decks.findByPlayerId(id);if(a.size()>=3)throw bad("每位玩家最多可儲存 3 組卡組。");validateDeck(x.cardIds());Deck d=new Deck();d.player=player(id);d.name=blank(x.name())?"卡組 "+(a.size()+1):x.name().trim();d.cardIds=String.join(",",x.cardIds().stream().map(String::valueOf).toList());return decks.save(d);}
  @DeleteMapping("/decks/{id}") public void deleteDeck(@PathVariable Long id){decks.deleteById(id);}

  @GetMapping("/rooms") public List<Map<String,Object>> list(){clean();return rooms.findAll().stream().map(this::view).toList();}
  @GetMapping("/rooms/{id}") public Map<String,Object> room(@PathVariable Long id){return view(roomById(id));}
  @PostMapping("/rooms") public Map<String,Object> create(@RequestBody RoomRequest x){clean();if(rooms.count()>=10)throw bad("目前最多只能建立 10 個房間。");if(x.turnSeconds()<30||x.turnSeconds()>60)throw bad("回合秒數必須介於 30 至 60 秒。");Room r=new Room();r.host=player(x.hostId());r.title=blank(x.title())?r.host.username+" 的房間":x.title().trim();r.ruleName=blank(x.ruleName())?"STANDARD":x.ruleName();r.password=blank(x.password())?null:x.password();r.turnSeconds=x.turnSeconds();return view(rooms.save(r));}
  @PostMapping("/rooms/{id}/join") public Map<String,Object> join(@PathVariable Long id,@RequestBody JoinRequest x){Room r=roomById(id);if(!Objects.equals(r.password,blank(x.password())?null:x.password()))throw bad("房間密碼錯誤。");if(!"WAITING".equals(r.status))throw bad("遊戲已經開始。");if(r.guest!=null&&!r.guest.id.equals(x.playerId()))throw bad("房間人數已滿。");if(!r.host.id.equals(x.playerId())){ownedDeck(x.playerId(),x.deckId());r.guest=player(x.playerId());r.guestDeckId=x.deckId();r.guestReady=false;r.expiresAt=Instant.now().plusSeconds(240);rooms.save(r);}return view(r);}
  @PostMapping("/rooms/{id}/deck") public Map<String,Object> chooseDeck(@PathVariable Long id,@RequestBody DeckChoice x){Room r=roomById(id);ownedDeck(x.playerId(),x.deckId());if(r.host.id.equals(x.playerId())){if(r.hostReady)throw bad("已準備後不能更換卡組。");r.hostDeckId=x.deckId();}else{guest(r,x.playerId());if(r.guestReady)throw bad("已準備後不能更換卡組。");r.guestDeckId=x.deckId();}return view(rooms.save(r));}
  @PostMapping("/rooms/{id}/ready") public Map<String,Object> ready(@PathVariable Long id,@RequestBody Action x){Room r=roomById(id);if(r.host.id.equals(x.playerId())){if(blank(r.hostDeckId))throw bad("請先選擇卡組。");r.hostReady=x.value();}else{guest(r,x.playerId());if(blank(r.guestDeckId))throw bad("請先選擇卡組。");r.guestReady=x.value();}return view(rooms.save(r));}
  @PostMapping("/rooms/{id}/start") public Map<String,Object> start(@PathVariable Long id,@RequestBody Action x){Room r=roomById(id);if(!r.host.id.equals(x.playerId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"只有房主可以開始遊戲。");if(r.guest==null||!r.hostReady||!r.guestReady)throw bad("雙方選擇卡組並準備完成後才能開始。");r.status="PLAYING";r.gameState=write(newGame(r));return view(rooms.save(r));}
  @PostMapping("/rooms/{id}/state") public Map<String,Object> state(@PathVariable Long id,@RequestBody StateRequest x){Room r=roomById(id);participant(r,x.playerId());Map<String,Object> now=read(r);if(now==null||!"PLAYING".equals(r.status))throw bad("遊戲尚未開始。");timeout(r,now);if(Boolean.TRUE.equals(now.get("finished")))return view(r);String mine=r.host.id.equals(x.playerId())?"blue":"red";if(!mine.equals(now.get("turn")))throw bad("尚未輪到你出牌。");Map<String,Object> next=read(x.state());if(next==null||!(next.get("board") instanceof List<?> board)||board.size()!=9)throw bad("無效的遊戲狀態。");String expected="blue".equals(mine)?"red":"blue";if(!expected.equals(next.get("turn")))throw bad("無效的回合狀態。");next.put("deadlineEpochMs",Instant.now().plusSeconds(r.turnSeconds).toEpochMilli());next.put("finished",false);r.gameState=write(next);return view(rooms.save(r));}
  @PostMapping("/rooms/{id}/leave") public Map<String,Object> leave(@PathVariable Long id,@RequestBody Action x){Room r=roomById(id);if(r.host.id.equals(x.playerId())){rooms.delete(r);return Map.of("disbanded",true);}guest(r,x.playerId());r.guest=null;r.guestDeckId=null;r.guestReady=false;r.hostReady=false;r.status="WAITING";r.gameState=null;r.expiresAt=Instant.now().plusSeconds(240);return view(rooms.save(r));}
  @DeleteMapping("/rooms/{id}/host/{player}") public void close(@PathVariable Long id,@PathVariable Long player){Room r=roomById(id);if(!r.host.id.equals(player))throw new ResponseStatusException(HttpStatus.FORBIDDEN);rooms.delete(r);}
  @DeleteMapping("/rooms/host/{id}") public void closeHost(@PathVariable Long id){rooms.findAll().stream().filter(r->r.host.id.equals(id)).forEach(rooms::delete);}
  @PostMapping("/battle/ai") public Map<String,Object> ai(@RequestBody AiRequest x){return Map.of("battleId","ai-"+UUID.randomUUID(),"ruleName",x.ruleName(),"turnSeconds",60,"opponent","AI");}

  private Map<String,Object> newGame(Room r){Map<String,Object>s=new LinkedHashMap<>();s.put("board",new ArrayList<>(Collections.nCopies(9,null)));s.put("turn",new Random().nextBoolean()?"blue":"red");s.put("deadlineEpochMs",Instant.now().plusSeconds(r.turnSeconds).toEpochMilli());s.put("finished",false);return s;}
  private void clean(){Instant n=Instant.now();rooms.findAll().forEach(r->{if("PLAYING".equals(r.status))timeout(r,read(r));if(r.guest==null&&r.expiresAt!=null&&r.expiresAt.isBefore(n))rooms.delete(r);});}
  private void timeout(Room r,Map<String,Object>s){if(s==null||Boolean.TRUE.equals(s.get("finished")))return;Object d=s.get("deadlineEpochMs");if(!(d instanceof Number deadline)||Instant.now().toEpochMilli()<deadline.longValue())return;String loser=String.valueOf(s.get("turn"));s.put("finished",true);s.put("winner","blue".equals(loser)?"red":"blue");s.put("reason","timeout");r.gameState=write(s);rooms.save(r);}
  private Map<String,Object> view(Room r){if("PLAYING".equals(r.status))timeout(r,read(r));Map<String,Object>m=new LinkedHashMap<>();m.put("id",r.id);m.put("title",r.title);m.put("ruleName",r.ruleName);m.put("turnSeconds",r.turnSeconds);m.put("hostId",r.host.id);m.put("hostName",r.host.username);m.put("guestId",r.guest==null?null:r.guest.id);m.put("guestName",r.guest==null?null:r.guest.username);m.put("hostDeckId",r.hostDeckId==null?"":r.hostDeckId);m.put("guestDeckId",r.guestDeckId==null?"":r.guestDeckId);m.put("hostReady",r.hostReady);m.put("guestReady",r.guestReady);m.put("status",r.status);m.put("passwordRequired",!blank(r.password));m.put("expiresAt",r.expiresAt.toString());m.put("gameState",r.gameState==null?"":r.gameState);return m;}
  private String username(String s){if(s==null||!USERNAME.matcher(s.trim()).matches())throw bad("帳號須為 3 至 32 個英數、中文、底線或連字號。");return s.trim();}
  private boolean matches(String raw,String stored){if(raw==null||stored==null)return false;if(stored.startsWith("$2a$")||stored.startsWith("$2b$")||stored.startsWith("$2y$"))return encoder.matches(raw,stored);return MessageDigest.isEqual(raw.getBytes(StandardCharsets.UTF_8),stored.getBytes(StandardCharsets.UTF_8));}
  private void ownedDeck(Long p,String id){try{if(decks.findById(Long.valueOf(id)).filter(d->d.player.id.equals(p)).isEmpty())throw bad("無效的卡組。");}catch(NumberFormatException e){throw bad("無效的卡組。");}}
  private void validateDeck(List<Long>x){if(x==null||x.size()!=5||new HashSet<>(x).size()!=5||cards.findAllById(x).size()!=5)throw bad("卡組必須包含 5 張不同的卡牌。");}
  private Player player(Long id){return players.findById(id).orElseThrow(()->bad("找不到玩家。"));} private Room roomById(Long id){Room r=rooms.findById(id).orElseThrow(()->bad("找不到房間。"));if("PLAYING".equals(r.status))timeout(r,read(r));return r;} private void guest(Room r,Long p){if(r.guest==null||!r.guest.id.equals(p))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"你不是此房間的加入者。");} private void participant(Room r,Long p){if(!r.host.id.equals(p)&&(r.guest==null||!r.guest.id.equals(p)))throw new ResponseStatusException(HttpStatus.FORBIDDEN);} private Map<String,Object> player(Player p,boolean t){return Map.of("id",p.id,"username",p.username,"showTutorial",t);}
  private Map<String,Object> read(Room r){return r.gameState==null||r.gameState.isBlank()?null:read(r.gameState);} private Map<String,Object> read(String s){try{return json.readValue(s,new TypeReference<>(){});}catch(Exception e){return null;}} private String write(Map<String,Object>s){try{return json.writeValueAsString(s);}catch(Exception e){throw new IllegalStateException(e);}} private boolean blank(String s){return s==null||s.isBlank();} private ResponseStatusException bad(String s){return new ResponseStatusException(HttpStatus.BAD_REQUEST,s);}
  record Auth(String username,String password){} record DeckRequest(String name,List<Long> cardIds){} record RoomRequest(Long hostId,String title,String ruleName,String password,int turnSeconds){} record JoinRequest(String password,Long playerId,String deckId){} record DeckChoice(Long playerId,String deckId){} record Action(Long playerId,boolean value){} record StateRequest(Long playerId,String state){} record AiRequest(String ruleName){}
}

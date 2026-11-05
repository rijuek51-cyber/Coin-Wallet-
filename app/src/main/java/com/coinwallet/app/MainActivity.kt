package com.coinwallet.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF050A18)
private val Surface = Color(0xFF0E172B)
private val Surface2 = Color(0xFF14213A)
private val Stroke = Color(0xFF24334D)
private val Cyan = Color(0xFF16B8F2)
private val TextMuted = Color(0xFF91A0B8)
private val Up = Color(0xFF51D6B0)
private val Down = Color(0xFFEF8CA6)

data class Asset(val name:String,val symbol:String,val price:String,val change:String,val up:Boolean,val glyph:String)
private val assets = listOf(
    Asset("Bitcoin","BTC","$80,184.00","+2.52%",true,"₿"),
    Asset("Ethereum","ETH","$2,416.08","+4.10%",true,"◆"),
    Asset("Tether","USDT","$0.9994","+0.04%",true,"₮"),
    Asset("BNB","BNB","$714.37","+2.04%",true,"⬡"),
    Asset("Solana","SOL","$96.30","+3.30%",true,"≋"),
    Asset("XRP","XRP","$1.32","+2.42%",true,"𝕏"),
    Asset("USDC","USDC","$0.9997","+0.03%",true,"＄"),
    Asset("Cardano","ADA","$0.1972","+3.38%",true,"●"),
    Asset("TRON","TRX","$0.3183","-0.17%",false,"△"),
    Asset("JUST","JST","$0.1426","+1.41%",true,"J"),
    Asset("Dogecoin","DOGE","$0.0767","+2.19%",true,"Ð"),
    Asset("Shiba Inu","SHIB","$0.00000476","+1.85%",true,"🐕"),
    Asset("Pepe","PEPE","$0.00000494","-1.28%",false,"🐸"),
    Asset("Bonk","BONK","$0.00000302","-0.95%",false,"🐶"),
    Asset("Chainlink","LINK","$11.50","+3.32%",true,"⬡"),
    Asset("Avalanche","AVAX","$8.72","+0.88%",true,"▲")
)

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent { CoinWalletApp() }
    }
}

@Composable
private fun CoinWalletApp(){
    var authenticated by remember { mutableStateOf(false) }
    var createMode by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("Home") }
    var selected by remember { mutableStateOf(assets[9]) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    MaterialTheme(colorScheme=darkColorScheme(
        background=Bg, surface=Surface, primary=Cyan,
        onBackground=Color.White, onSurface=Color.White
    )){
        Surface(Modifier.fillMaxSize(),color=Bg){
            if(!authenticated) LoginScreen(createMode,{createMode=it},{authenticated=true})
            else when(page){
                "Asset" -> AssetScreen(selected){page="Home"}
                "Receive" -> ReceiveScreen{page="Home"}
                "Send" -> SendScreen{page="Home"}
                "Swap" -> SwapScreen{page="Home"}
                "Activity" -> ActivityScreen{page="Home"}
                "Address book" -> ContactsScreen{page="Home"}
                "Alerts" -> AlertsScreen{page="Home"}
                "Settings" -> SettingsScreen{page="Home"}
                "Add token" -> AddTokenScreen{page="Home"}
                else -> Dashboard(
                    search,{search=it},filter,{filter=it},
                    {selected=it;page="Asset"},{page=it},{authenticated=false}
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(create:Boolean,onMode:(Boolean)->Unit,onEnter:()->Unit){
    var email by remember{mutableStateOf("")}
    var password by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().padding(22.dp),verticalArrangement=Arrangement.Center){
        Row(verticalAlignment=Alignment.CenterVertically){
            BrandMark(54)
            Spacer(Modifier.width(13.dp))
            Column { Text("Coin Wallet",fontSize=26.sp,fontWeight=FontWeight.Bold);Text("Your crypto overview",color=TextMuted) }
        }
        Spacer(Modifier.height(34.dp))
        PanelCard {
            Row(Modifier.fillMaxWidth().background(Bg,CircleShape).padding(4.dp)){
                Segment("Sign in",!create,Modifier.weight(1f)){onMode(false)}
                Segment("Create account",create,Modifier.weight(1f)){onMode(true)}
            }
            Spacer(Modifier.height(20.dp))
            Text("EMAIL ADDRESS",color=TextMuted,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
            OutlinedTextField(email,{email=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("name@example.com")},singleLine=true,shape=RoundedCornerShape(15.dp))
            Spacer(Modifier.height(13.dp))
            Text("PASSWORD",color=TextMuted,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
            OutlinedTextField(password,{password=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("At least 8 characters")},visualTransformation=PasswordVisualTransformation(),singleLine=true,shape=RoundedCornerShape(15.dp))
            Spacer(Modifier.height(20.dp))
            Button(onClick=onEnter,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Cyan)){
                Text(if(create)"Continue in demo" else "Sign in to demo",color=Bg,fontWeight=FontWeight.Bold,fontSize=16.sp)
            }
        }
        Spacer(Modifier.height(15.dp))
        Text("Demo interface only. No credentials are sent or saved.",color=TextMuted,fontSize=12.sp)
    }
}

@Composable
private fun Dashboard(search:String,onSearch:(String)->Unit,filter:String,onFilter:(String)->Unit,onAsset:(Asset)->Unit,onPage:(String)->Unit,onLogout:()->Unit){
    val visible=assets.filter{("${it.name} ${it.symbol}").contains(search,true)}.filter{
        when(filter){
            "TRON"->it.name=="TRON"||it.name=="JUST"
            "Meme"->it.name in listOf("Dogecoin","Shiba Inu","Pepe","Bonk")
            "Major"->it.name in listOf("Bitcoin","Ethereum","BNB","Solana","XRP")
            else->true
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=17.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(top=22.dp,bottom=30.dp)){
        item{
            Row(verticalAlignment=Alignment.CenterVertically){
                BrandMark(43);Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)){Text("Coin Wallet",fontSize=21.sp,fontWeight=FontWeight.Bold);Text("Portfolio overview",color=TextMuted,fontSize=13.sp)}
                IconButton(onClick={onPage("Activity")}){Icon(Icons.Default.ReceiptLong,null,tint=TextMuted)}
                IconButton(onClick=onLogout){Icon(Icons.Default.Logout,null,tint=TextMuted)}
            }
        }
        item{
            PanelCard(gradient=true){
                Text("TOTAL WALLET VALUE",color=TextMuted,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text("$89.79",fontSize=39.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                    Tag("+1.45% 24h",Up)
                }
                Text("Across 1 sample holding",color=TextMuted)
                TextButton(onClick={onPage("Activity")}){Text("View activity")}
            }
        }
        item{
            PanelCard{
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text("🔥",fontSize=23.sp);Spacer(Modifier.width(8.dp))
                    Column{Text("Meme movers",fontWeight=FontWeight.Bold,fontSize=19.sp);Text("Biggest 24h moves",color=TextMuted,fontSize=13.sp)}
                }
                Text("↗  TOP GAINERS",color=Up,fontWeight=FontWeight.Bold,fontSize=13.sp)
                listOf(assets[12],assets[10],assets[11]).forEach{MoverRow(it,onAsset)}
                Text("↘  TOP DECLINERS",color=Down,fontWeight=FontWeight.Bold,fontSize=13.sp)
                listOf(assets[13],assets[12]).forEach{MoverRow(it,onAsset)}
            }
        }
        item{
            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){
                ActionTile("Receive",Icons.Default.ArrowDownward,Modifier.weight(1f)){onPage("Receive")}
                ActionTile("Send",Icons.Default.ArrowUpward,Modifier.weight(1f)){onPage("Send")}
                ActionTile("Swap",Icons.Default.SwapHoriz,Modifier.weight(1f)){onPage("Swap")}
            }
        }
        item{
            Row(Modifier.fillMaxWidth().background(Surface,RoundedCornerShape(30.dp)).padding(4.dp)){
                listOf("All","Major","TRON","Meme").forEach{Segment(it,filter==it,Modifier.weight(1f)){onFilter(it)}}
            }
        }
        item{
            OutlinedTextField(search,onSearch,modifier=Modifier.fillMaxWidth(),placeholder={Text("Search assets",color=TextMuted)},leadingIcon={Icon(Icons.Default.Search,null,tint=TextMuted)},singleLine=true,shape=CircleShape)
        }
        items(visible){asset->AssetRow(asset){onAsset(asset)}}
        item{
            PanelCard{
                Text("WALLET TOOLS",fontSize=12.sp,color=TextMuted,fontWeight=FontWeight.Bold)
                ToolItem("Transaction history",Icons.Default.ReceiptLong){onPage("Activity")}
                ToolItem("Address book",Icons.Default.Contacts){onPage("Address book")}
                ToolItem("Price alerts",Icons.Default.NotificationsActive){onPage("Alerts")}
                ToolItem("Add custom token",Icons.Default.AddCircleOutline){onPage("Add token")}
                ToolItem("Settings & security",Icons.Default.Settings){onPage("Settings")}
            }
        }
        item{Text("Market figures and portfolio values are illustrative sample data.",color=TextMuted,fontSize=11.sp)}
    }
}

@Composable private fun AssetScreen(asset:Asset,back:()->Unit){
    Column(Modifier.fillMaxSize().padding(18.dp)){
        BackBar("All assets",back);Spacer(Modifier.height(28.dp))
        Row(verticalAlignment=Alignment.CenterVertically){CoinIcon(asset);Spacer(Modifier.width(13.dp));Column{Text(asset.symbol+" · MARKET",color=TextMuted);Text(asset.name,fontSize=30.sp,fontWeight=FontWeight.Bold)}}
        Spacer(Modifier.height(25.dp));Text(asset.price,fontSize=42.sp,fontWeight=FontWeight.Bold);Tag(asset.change+" 24h",if(asset.up)Up else Down)
        Spacer(Modifier.height(22.dp))
        PanelCard{
            Text("Price history",fontSize=19.sp,fontWeight=FontWeight.Bold)
            Text("1D       7D       1M       3M       1Y",color=Cyan)
            Spacer(Modifier.height(35.dp))
            Text("Connect a verified market-data source to display live charts.",color=TextMuted)
            Spacer(Modifier.height(60.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("24h high","—",Modifier.weight(1f));Metric("24h low","—",Modifier.weight(1f))}
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("Market cap","—",Modifier.weight(1f));Metric("24h volume","—",Modifier.weight(1f))}
    }
}

@Composable private fun ReceiveScreen(back:()->Unit){
    val ctx=LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp)){
        BackBar("Receive",back)
        PanelCard{
            Icon(Icons.Default.QrCode2,null,tint=Cyan,modifier=Modifier.size(70.dp).align(Alignment.CenterHorizontally))
            Text("Receive crypto",fontSize=22.sp,fontWeight=FontWeight.Bold)
            Text("Select a network and use the matching address. A real address is not available in this prototype.",color=TextMuted)
            Button(onClick={Toast.makeText(ctx,"No wallet address configured",Toast.LENGTH_LONG).show()},modifier=Modifier.fillMaxWidth()){Text("Copy address · unavailable")}
        }
        Text("Always verify the network before receiving. Never share your recovery phrase.",color=TextMuted)
    }
}

@Composable private fun SendScreen(back:()->Unit){
    val ctx=LocalContext.current
    var recipient by remember{mutableStateOf("")};var amount by remember{mutableStateOf("")};var network by remember{mutableStateOf("TRON")}
    Column(Modifier.fillMaxSize().padding(20.dp)){
        BackBar("Send crypto",back)
        PanelCard{
            Text("Recipient address",color=TextMuted)
            OutlinedTextField(recipient,{recipient=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Paste wallet address")})
            Text("Network",color=TextMuted)
            var expanded by remember{mutableStateOf(false)}
            Box{OutlinedButton(onClick={expanded=true}){Text(network);Icon(Icons.Default.ArrowDropDown,null)}
                DropdownMenu(expanded,{expanded=false}){listOf("TRON","Ethereum","Solana","BNB Chain").forEach{DropdownMenuItem(text={Text(it)},onClick={network=it;expanded=false})}}}
            Text("Amount",color=TextMuted)
            OutlinedTextField(amount,{amount=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("0.00")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
            Button(onClick={Toast.makeText(ctx,"Demo only: no transaction is signed or sent",Toast.LENGTH_LONG).show()},modifier=Modifier.fillMaxWidth()){Text("Review transfer")}
        }
        Text("Check the recipient and network carefully. Sending crypto is generally irreversible.",color=TextMuted,fontSize=12.sp)
    }
}

@Composable private fun SwapScreen(back:()->Unit){
    val ctx=LocalContext.current;var amount by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().padding(20.dp)){BackBar("Swap assets",back)
        PanelCard{
            Text("From · USDT",color=TextMuted);OutlinedTextField(amount,{amount=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Amount")})
            Icon(Icons.Default.SwapVert,null,tint=Cyan,modifier=Modifier.align(Alignment.CenterHorizontally).size(32.dp))
            Text("To · TRX",color=TextMuted);Text("Estimated output: unavailable",color=TextMuted)
            Button(onClick={Toast.makeText(ctx,"A verified DEX quote provider is required",Toast.LENGTH_LONG).show()},modifier=Modifier.fillMaxWidth()){Text("Get quote")}
        }
    }
}

@Composable private fun ActivityScreen(back:()->Unit){
    Column(Modifier.fillMaxSize().padding(20.dp)){BackBar("Activity",back);PanelCard{
        Text("Recent activity",fontSize=21.sp,fontWeight=FontWeight.Bold)
        listOf("Received · example","+$25.00 USDT","Sent · example","−10.00 TRX","Swap · example","USDT → TRX").chunked(2).forEach{
            HorizontalDivider(color=Stroke);Text(it[0],fontWeight=FontWeight.SemiBold);Text(it[1],color=TextMuted)
        }
        Text("Example entries only; no blockchain history is connected.",color=TextMuted,fontSize=12.sp)
    }}
}

@Composable private fun ContactsScreen(back:()->Unit){
    val ctx=LocalContext.current;var name by remember{mutableStateOf("")};var addr by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().padding(20.dp)){BackBar("Address book",back);PanelCard{
        Text("Add trusted contact",fontSize=20.sp,fontWeight=FontWeight.Bold)
        OutlinedTextField(name,{name=it},modifier=Modifier.fillMaxWidth(),label={Text("Contact name")})
        OutlinedTextField(addr,{addr=it},modifier=Modifier.fillMaxWidth(),label={Text("Wallet address")})
        Button(onClick={Toast.makeText(ctx,"Contact storage not enabled in prototype",Toast.LENGTH_SHORT).show()},modifier=Modifier.fillMaxWidth()){Text("Save contact")}
    }}
}

@Composable private fun AlertsScreen(back:()->Unit){
    var on by remember{mutableStateOf(true)};val ctx=LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp)){BackBar("Price alerts",back);PanelCard{
        Text("Alert preferences",fontSize=20.sp,fontWeight=FontWeight.Bold)
        Row(verticalAlignment=Alignment.CenterVertically){Text("Enable alerts",Modifier.weight(1f));Switch(on,{on=it})}
        Text("Price alert delivery requires a live market-data and notification service.",color=TextMuted)
        OutlinedButton(onClick={Toast.makeText(ctx,"Alert provider not connected",Toast.LENGTH_SHORT).show()}){Text("Create alert")}
    }}
}

@Composable private fun AddTokenScreen(back:()->Unit){
    val ctx=LocalContext.current;var contract by remember{mutableStateOf("")};var network by remember{mutableStateOf("TRON")}
    Column(Modifier.fillMaxSize().padding(20.dp)){BackBar("Add custom token",back);PanelCard{
        var expanded by remember{mutableStateOf(false)}
        Text("Network",color=TextMuted)
        Box{OutlinedButton(onClick={expanded=true}){Text(network);Icon(Icons.Default.ArrowDropDown,null)}
            DropdownMenu(expanded,{expanded=false}){listOf("TRON","Ethereum","Solana","BNB Chain").forEach{DropdownMenuItem(text={Text(it)},onClick={network=it;expanded=false})}}}
        OutlinedTextField(contract,{contract=it},modifier=Modifier.fillMaxWidth(),label={Text("Contract / mint address")})
        Button(onClick={Toast.makeText(ctx,"Token verification service not connected",Toast.LENGTH_SHORT).show()},modifier=Modifier.fillMaxWidth()){Text("Verify token")}
    }}
}

@Composable private fun SettingsScreen(back:()->Unit){
    var biometric by remember{mutableStateOf(false)};var hide by remember{mutableStateOf(true)};val ctx=LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp)){BackBar("Settings & security",back);PanelCard{
        Text("Security preferences",fontSize=20.sp,fontWeight=FontWeight.Bold)
        Row(verticalAlignment=Alignment.CenterVertically){Text("Biometric unlock (UI demo)",Modifier.weight(1f));Switch(biometric,{biometric=it})}
        Row(verticalAlignment=Alignment.CenterVertically){Text("Hide balances",Modifier.weight(1f));Switch(hide,{hide=it})}
        HorizontalDivider(color=Stroke)
        ToolItem("Backup & recovery guidance",Icons.Default.Security){Toast.makeText(ctx,"Secure key backup must be implemented and audited before release",Toast.LENGTH_LONG).show()}
        ToolItem("Network preferences",Icons.Default.Lan){Toast.makeText(ctx,"Network settings are demo-only",Toast.LENGTH_SHORT).show()}
        ToolItem("Privacy information",Icons.Default.Policy){Toast.makeText(ctx,"Add a reviewed privacy policy before public release",Toast.LENGTH_SHORT).show()}
    }
    Text("No private keys or seed phrases are generated, imported, stored or transmitted.",color=TextMuted,fontSize=12.sp)}
}

@Composable private fun PanelCard(gradient:Boolean=false,content:@Composable ColumnScope.()->Unit){
    Card(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(25.dp),
        colors=CardDefaults.cardColors(containerColor=if(gradient)Color(0xFF10263B) else Surface),
        border=androidx.compose.foundation.BorderStroke(1.dp,Stroke)
    ){Column(Modifier.padding(19.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}
}
@Composable private fun BrandMark(size:Int){
    Box(Modifier.size(size.dp).background(Color(0xFF082B43),RoundedCornerShape(17.dp)),contentAlignment=Alignment.Center){
        Icon(Icons.Default.AccountBalanceWallet,null,tint=Color(0xFF7DD8FF),modifier=Modifier.size((size*.55).dp))
    }
}
@Composable private fun Segment(text:String,active:Boolean,modifier:Modifier=Modifier,onClick:()->Unit){
    Box(modifier.background(if(active)Cyan else Color.Transparent,CircleShape).clickable{onClick()}.padding(vertical=11.dp,horizontal=4.dp),contentAlignment=Alignment.Center){
        Text(text,color=if(active)Bg else TextMuted,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
    }
}
@Composable private fun Tag(text:String,color:Color){
    Text(text,color=color,fontWeight=FontWeight.Bold,modifier=Modifier.background(color.copy(alpha=.13f),CircleShape).padding(horizontal=11.dp,vertical=7.dp))
}
@Composable private fun CoinIcon(asset:Asset){
    Box(Modifier.size(48.dp).background(Surface2,CircleShape),contentAlignment=Alignment.Center){Text(asset.glyph,fontSize=23.sp,color=Cyan,fontWeight=FontWeight.Bold)}
}
@Composable private fun AssetRow(asset:Asset,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().background(Surface,RoundedCornerShape(21.dp)).clickable{onClick()}.padding(15.dp),verticalAlignment=Alignment.CenterVertically){
        CoinIcon(asset);Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)){Text(asset.name,fontSize=17.sp,fontWeight=FontWeight.Bold);Text(asset.symbol,color=TextMuted)}
        Column(horizontalAlignment=Alignment.End){Text(asset.price,fontWeight=FontWeight.Bold,fontSize=16.sp);Text(asset.change,color=if(asset.up)Up else Down)}
    }
}
@Composable private fun MoverRow(asset:Asset,onClick:(Asset)->Unit){
    Row(Modifier.fillMaxWidth().clickable{onClick(asset)}.padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){
        CoinIcon(asset);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(asset.name,fontWeight=FontWeight.SemiBold);Text(asset.price,color=TextMuted,fontSize=12.sp)};Tag(asset.change,if(asset.up)Up else Down)
    }
}
@Composable private fun ActionTile(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier=Modifier,onClick:()->Unit){
    Card(modifier.clickable{onClick()},colors=CardDefaults.cardColors(containerColor=Surface),border=androidx.compose.foundation.BorderStroke(1.dp,Stroke),shape=RoundedCornerShape(20.dp)){
        Column(Modifier.fillMaxWidth().padding(vertical=17.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,tint=Color(0xFF7DD8FF),modifier=Modifier.size(27.dp));Spacer(Modifier.height(7.dp));Text(label)}
    }
}
@Composable private fun ToolItem(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().clickable{onClick()}.padding(vertical=11.dp),verticalAlignment=Alignment.CenterVertically){
        Icon(icon,null,tint=Cyan);Spacer(Modifier.width(12.dp));Text(label,Modifier.weight(1f));Icon(Icons.Default.ChevronRight,null,tint=TextMuted)
    }
}
@Composable private fun BackBar(title:String,back:()->Unit){
    Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick=back){Icon(Icons.Default.ArrowBack,null);Text(" Back")};Text(title,fontSize=21.sp,fontWeight=FontWeight.Bold)}
}
@Composable private fun Metric(title:String,value:String,modifier:Modifier=Modifier){
    Card(modifier,colors=CardDefaults.cardColors(containerColor=Surface),border=androidx.compose.foundation.BorderStroke(1.dp,Stroke)){
        Column(Modifier.padding(15.dp)){Text(title,color=TextMuted);Text(value,fontWeight=FontWeight.Bold,fontSize=19.sp)}
    }
}

/* ATOMIC TV Console © 2026 JB Atomic Industries. No remote scripts or third-party trackers. */
(()=>{'use strict';
const $=id=>document.getElementById(id);
const state={lang:localStorage.getItem('atomic-console-lang')==='en'?'en':'fr',csrf:'',catalog:[],tasks:[],page:'workflows',currentJob:null,poll:null};
const words={
fr:{navigation:'NAVIGATION',workflows:'Actions groupées',commands:'Commandes ADB',terminal:'Terminal',history:'Résultats et exports',logout:'Déconnexion',loginTitle:'Connexion sécurisée',loginHelp:'Saisissez le code à 4 chiffres affiché sur le téléviseur. Uniquement sur votre réseau local.',pin:'Code TV',connect:'Se connecter',lanWarning:"Le trafic HTTP local n'est pas chiffré : utilisez uniquement un réseau privé de confiance.",workflowTitle:'Optimisation et diagnostic',workflowLead:'Enchaînez des commandes dans un ordre contrôlé. Les réglages sont signalés.',catalogTitle:'Toutes les commandes',catalogLead:'Recherchez ou filtrez par catégorie.',terminalTitle:'Terminal ADB',terminalLead:'Saisissez une commande adb shell. Toute commande libre nécessite une confirmation.',shellCommand:'Commande Android shell',run:'Exécuter',terminalWarning:'⚠ Vérifiez vos commandes : elles utilisent les autorisations ADB du téléviseur.',historyTitle:'Historique des résultats',historyLead:'Copiez les résultats ou téléchargez un rapport TXT.',refresh:'Actualiser',copy:'Copier',download:'Télécharger .txt',all:'Toutes les catégories',status:'Connecté à la TV',offline:'Connexion requise',confirm:'Voulez-vous exécuter cette opération sur la TV ?',danger:'⚠ Cette commande peut modifier des réglages ou des données. Confirmer ?',noResults:'Aucun résultat.',running:'EN COURS',success:'TERMINÉ',failure:'ÉCHEC',start:'Lancer',search:'Rechercher une commande…',results:'résultats',busy:'Une commande est déjà en cours.',error:'Erreur de connexion',report:'Résultats',empty:'Aucune exécution enregistrée.',copySuccess:'Résultat copié.',signedOff:'Session fermée.'},
en:{navigation:'NAVIGATION',workflows:'Workflows',commands:'ADB commands',terminal:'Terminal',history:'Results and exports',logout:'Sign out',loginTitle:'Secure sign-in',loginHelp:'Enter the 4-digit PIN shown on your TV. Only available on your local network.',pin:'TV PIN',connect:'Sign in',lanWarning:'Local HTTP traffic is not encrypted. Use only a trusted private network.',workflowTitle:'Optimization and diagnostics',workflowLead:'Run commands in a controlled sequence. System changes are flagged.',catalogTitle:'All commands',catalogLead:'Search or filter by category.',terminalTitle:'ADB terminal',terminalLead:'Type an adb shell command. Every custom command requires confirmation.',shellCommand:'Android shell command',run:'Run',terminalWarning:'⚠ Check your commands: they use the TV’s ADB permissions.',historyTitle:'Result history',historyLead:'Copy command output or download a TXT report.',refresh:'Refresh',copy:'Copy',download:'Download .txt',all:'All categories',status:'Connected to TV',offline:'Sign-in required',confirm:'Run this operation on the TV?',danger:'⚠ This command can change settings or data. Continue?',noResults:'No results.',running:'RUNNING',success:'COMPLETED',failure:'FAILED',start:'Run',search:'Search commands…',results:'results',busy:'A command is already running.',error:'Connection error',report:'Results',empty:'No command history yet.',copySuccess:'Output copied.',signedOff:'Signed out.'}};
const tr=k=>words[state.lang][k]||k;
function el(tag,cls,txt){const e=document.createElement(tag);if(cls)e.className=cls;if(txt!==undefined)e.textContent=txt;return e}
function h(id,text){$(id).textContent=text}
function language(){
 $('language').value=state.lang;document.documentElement.lang=state.lang;
 document.querySelectorAll('[data-i18n]').forEach(e=>{const k=e.getAttribute('data-i18n');e.textContent=tr(k)});
 $('search').placeholder=tr('search');
 $('statusText').textContent=state.csrf?tr('status'):tr('offline');
 const titles={workflows:'workflows',commands:'commands',terminal:'terminal',history:'history'};
 h('pageTitle',tr(titles[state.page]));
 render();try{localStorage.setItem('atomic-console-lang',state.lang)}catch(e){}
}
async function api(path,params=null){
 const init={credentials:'same-origin',cache:'no-store',headers:{}};
 if(params){init.method='POST';init.headers['Content-Type']='application/x-www-form-urlencoded;charset=UTF-8';init.headers['X-Atomic-Csrf']=state.csrf;init.body=new URLSearchParams(params).toString()}
 let res=await fetch(path,init), data;
 try{data=await res.json()}catch(e){throw Error('Invalid server response')}
 if(!res.ok)throw Error(data.error||'HTTP '+res.status);
 return data;
}
function setLogged(logged){$('login').hidden=logged;$('dashboard').hidden=!logged;$('logout').hidden=!logged;$('statusDot').classList.toggle('ok',logged)}
async function loginSession(){
 try{const s=await api('/api/session');state.csrf=s.csrf;setLogged(true);$('metricVersion').textContent=s.version;await fetchData()}
 catch(e){state.csrf='';setLogged(false)}
 language();
}
async function fetchData(){
 const [catalog,tasks]=await Promise.all([api('/api/catalog'),api('/api/tasks')]);
 state.catalog=catalog;state.tasks=tasks;
 h('metricCommands',String(catalog.length));h('metricWorkflows',String(tasks.length));render();
}
function select(page){
 state.page=page;document.querySelectorAll('.nav-btn').forEach(e=>e.classList.toggle('selected',e.dataset.page===page));
 document.querySelectorAll('.page').forEach(e=>e.hidden=e.id!=='page-'+page);
 h('pageTitle',tr(page));render();
}
function render(){if(!state.csrf)return;renderTasks();renderCatalog();if(state.page==='history')loadHistory();}
function confirmRun(title,commands,sensitive){
 let message=title+'\n\n'+commands.map(x=>'$ adb shell '+x).join('\n');
 message+='\n\n'+(sensitive?tr('danger'):tr('confirm'));
 return window.confirm(message);
}
async function execute(params,title,commands,sensitive){
 if(!confirmRun(title,commands,sensitive))return;
 try{
 const data=await api('/api/run',{...params,confirm:sensitive?'yes':'no'});
 state.currentJob=data.id;
 $('resultPanel').hidden=false;h('resultTitle',title);h('resultState',tr('running'));
 $('resultState').className='pill warn';h('resultOutput','…');
 $('download').hidden=true;$('copy').disabled=true;
 $('resultPanel').scrollIntoView({behavior:'smooth',block:'nearest'});
 if(state.poll)clearTimeout(state.poll);
 await poll(data.id);
 }catch(e){window.alert(e.message||tr('error'))}
}
async function poll(id){
 try{
 const data=await api('/api/job?id='+encodeURIComponent(id));
 if(data.status==='running'){state.poll=setTimeout(()=>poll(id),900);return}
 display(data);
 }catch(e){window.alert(e.message)}
}
function display(data){
 state.currentJob=data.id;$('resultPanel').hidden=false;
 h('resultTitle',data.title||tr('report'));
 h('resultState',tr(data.status==='running'?'running':data.ok?'success':'failure'));
 $('resultState').className='pill '+(data.ok?'ok':'warn');
 h('resultOutput',data.output||'');
 $('copy').disabled=false;$('download').hidden=data.status!=='done';
 $('download').href='/api/export?id='+encodeURIComponent(data.id);
 $('resultPanel').scrollIntoView({behavior:'smooth',block:'nearest'});
}
function renderTasks(){
 const parent=$('workflowList');parent.replaceChildren();
 state.tasks.forEach(task=>{
 const card=el('article','card'),heading=el('h3','',task[state.lang]);
 const desc=el('p','',task[state.lang==='fr'?'descFr':'descEn']);
 const badge=el('span','pill'+(task.sensitive?' warn':''),task.sensitive?'⚠':'✓');
 const button=el('button','btn primary',tr('start'));button.type='button';
 button.addEventListener('click',()=>execute({mode:'workflow',id:String(task.id)},task[state.lang],task.commands,task.sensitive));
 card.append(badge,heading,desc,button);parent.append(card);
 });
}
function renderCatalog(){
 const select=$('category'),current=select.value;
 select.replaceChildren();
 select.append(new Option(tr('all'),'all'));
 const labels={system:['Système','System'],display:['Affichage','Display'],network:['Réseau','Network'],audio:['Audio','Audio'],apps:['Applications','Apps'],storage:['Stockage','Storage'],input:['Télécommande','Remote'],performance:['Performances','Performance'],settings:['Paramètres','Settings'],diagnostic:['Diagnostic','Diagnostics']};
 for(const group of [...new Set(state.catalog.map(c=>c.group))]){
   const label=labels[group]||[group,group];select.append(new Option(label[state.lang==='fr'?0:1],group));
 }
 select.value=current||'all';
 const q=$('search').value.trim().toLowerCase(),list=$('commandList');list.replaceChildren();
 let count=0;
 state.catalog.forEach(item=>{
 if(select.value!=='all'&&item.group!==select.value)return;
 const hay=[item.fr,item.en,item.descFr,item.descEn,item.command,item.group].join(' ').toLowerCase();
 if(q&&!hay.includes(q))return;count++;
 const row=el('div','command-row'),info=el('div','command-info');
 info.append(el('strong','',item[state.lang]),el('p','',item[state.lang==='fr'?'descFr':'descEn']),el('code','',item.command));
 const run=el('button','btn',tr('run'));run.type='button';
 run.addEventListener('click',()=>execute({mode:'catalog',id:String(item.id)},item[state.lang],[item.command],item.sensitive));
 row.append(info,run);list.append(row);
 });
 h('commandCount',count+' '+tr('results'));
 if(!count)list.append(el('p','hint',tr('noResults')));
}
async function loadHistory(){
 try{
 const data=await api('/api/history');const list=$('historyList');list.replaceChildren();
 if(!data.length){list.append(el('p','hint',tr('empty')));return}
 for(const item of data){
 const card=el('div','history-item'),info=el('div');
 info.append(el('strong','',item.title),el('small','',new Date(item.created).toLocaleString()+' · '+(item.ok?tr('success'):tr('failure'))));
 const open=el('button','btn subtle',tr('report'));open.type='button';
 open.addEventListener('click',async()=>{try{display(await api('/api/job?id='+encodeURIComponent(item.id)))}catch(e){window.alert(e.message)}});
 const download=el('a','btn subtle','.txt');download.href='/api/export?id='+encodeURIComponent(item.id);
 download.setAttribute('download','');card.append(info,open,download);list.append(card);
 }
 }catch(e){window.alert(e.message)}
}
$('language').addEventListener('change',e=>{state.lang=e.target.value;language()});
document.querySelectorAll('.nav-btn').forEach(b=>b.addEventListener('click',()=>select(b.dataset.page)));
$('loginForm').addEventListener('submit',async e=>{e.preventDefault();h('loginError','');
 try{
 const data=await api('/api/login',{pin:$('pin').value});state.csrf=data.csrf;
 setLogged(true);await loginSession();
 }catch(err){h('loginError',err.message)}
});
$('logout').addEventListener('click',async()=>{try{await api('/api/logout',{})}catch(e){}
 state.csrf='';setLogged(false);language()});
$('search').addEventListener('input',renderCatalog);
$('category').addEventListener('change',renderCatalog);
$('terminalForm').addEventListener('submit',e=>{
 e.preventDefault();const command=$('terminalInput').value.trim();
 if(!command||command.length>450||/[\r\n]/.test(command))return;
 execute({mode:'terminal',command},'Terminal',[command],true);
});
$('refresh').addEventListener('click',loadHistory);
$('copy').addEventListener('click',async()=>{
 try{await navigator.clipboard.writeText($('resultOutput').textContent);window.alert(tr('copySuccess'))}
 catch(e){
 const input=el('textarea');input.value=$('resultOutput').textContent;
 document.body.append(input);input.select();document.execCommand('copy');input.remove();window.alert(tr('copySuccess'));
 }
});
setLogged(false);select('workflows');language();loginSession();
})();
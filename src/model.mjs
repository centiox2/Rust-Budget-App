export const categories = ['Groceries','Dining','Transport','Shopping','Utilities','Health','Entertainment','Other'];
export const today = () => new Date().toLocaleDateString('en-CA');
export const monthNow = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`; };
export function cents(value) {
 if (!/^\d+(\.\d{1,2})?$/.test(String(value))) throw Error('Enter a positive amount with up to 2 decimals.');
 const amount = Math.round(Number(value)*100);
 if (!Number.isSafeInteger(amount) || amount <= 0 || amount > 100000000000) throw Error('Amount must be between 0.01 and 1 billion.');
 return amount;
}
export function summary(transactions, month) {
 const rows = transactions.filter(t=>t.date.startsWith(month));
 const income = rows.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
 const spent = rows.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
 return {income,spent,balance:transactions.reduce((s,t)=>s+(t.type==='income'?t.amount:-t.amount),0),byCategory:Object.fromEntries(categories.map(c=>[c,rows.filter(t=>t.type==='expense'&&t.category===c).reduce((s,t)=>s+t.amount,0)]))};
}
export function validate(data) {
 if (!data || data.version!==1 || typeof data.name!=='string' || data.name.length>50 || !['USD','KES','EUR','GBP','INR'].includes(data.currency) || !Array.isArray(data.transactions) || !data.budgets || typeof data.budgets!=='object' || Array.isArray(data.budgets) || typeof data.onboarded!=='boolean') throw Error('This is not a valid Verdant backup.');
 const ids = new Set();
 for (const t of data.transactions) {
  if (!t || typeof t.id!=='string' || ids.has(t.id) || !['income','expense'].includes(t.type) || !Number.isSafeInteger(t.amount) || t.amount<=0 || t.amount>100000000000 || typeof t.title!=='string' || !t.title.trim() || t.title.length>100 || !categories.includes(t.category) || typeof t.date!=='string' || !/^\d{4}-\d{2}-\d{2}$/.test(t.date) || isNaN(Date.parse(t.date)) || new Date(t.date).toISOString().slice(0,10)!==t.date) throw Error('Backup contains an invalid transaction.');
  ids.add(t.id);
 }
 for (const [month, limits] of Object.entries(data.budgets)) {
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(month) || !limits || typeof limits!=='object' || Array.isArray(limits)) throw Error('Invalid budget month.');
  for (const [category,limit] of Object.entries(limits)) if (!categories.includes(category) || !Number.isSafeInteger(limit) || limit<0 || limit>100000000000) throw Error('Invalid budget limit.');
 }
 return data;
}
export const emptyData = () => ({version:1,name:'',currency:'USD',onboarded:false,transactions:[],budgets:{}});
export function demoData(name, currency) {
 const month=monthNow();
 return {version:1,name,currency,onboarded:true,budgets:{[month]:{Groceries:50000,Dining:20000,Transport:18000,Shopping:30000,Utilities:25000}},transactions:[
 ['income','Monthly salary',420000,'Other',1],['expense','Fresh market',6840,'Groceries',2],['expense','Coffee & company',1250,'Dining',3],['expense','City commute',2400,'Transport',4],['expense','Weekly groceries',8235,'Groceries',5],['expense','Internet bill',5999,'Utilities',6],['expense','A little something',8900,'Shopping',7],['income','Freelance project',65000,'Other',8],['expense','Dinner with friends',4650,'Dining',9]
 ].map(([type,title,amount,category,day])=>({id:crypto.randomUUID(),type,title,amount,category,date:`${month}-${String(Math.min(Number(day),new Date().getDate())).padStart(2,'0')}`}))};
}

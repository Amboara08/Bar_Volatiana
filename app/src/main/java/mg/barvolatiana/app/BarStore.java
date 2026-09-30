package mg.barvolatiana.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/** Stockage local rétrocompatible avec Bar Volatiana 1.0. */
public class BarStore {
    private static final int SCHEMA_VERSION = 2;
    private final SharedPreferences prefs;
    public JSONArray products, sales, expenses, debts, suppliers, versements;
    public JSONArray clients, loans, debtPayments, calculatorHistory;

    public BarStore(Context context) {
        prefs = context.getSharedPreferences("bar_volatiana_data", Context.MODE_PRIVATE);
        products = read("products"); sales = read("sales"); expenses = read("expenses"); debts = read("debts");
        suppliers = read("suppliers"); versements = read("versements"); clients = read("clients");
        loans = read("loans"); debtPayments = read("debt_payments"); calculatorHistory = read("calculator_history");
        migrate();
    }

    private JSONArray read(String key) {
        try { return new JSONArray(prefs.getString(key, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    public void saveAll() {
        prefs.edit().putString("products", products.toString()).putString("sales", sales.toString())
            .putString("expenses", expenses.toString()).putString("debts", debts.toString())
            .putString("suppliers", suppliers.toString()).putString("versements", versements.toString())
            .putString("clients", clients.toString()).putString("loans", loans.toString())
            .putString("debt_payments", debtPayments.toString())
            .putString("calculator_history", calculatorHistory.toString())
            .putInt("schema_version", SCHEMA_VERSION).apply();
    }

    private void migrate() {
        try {
            for (int i = 0; i < products.length(); i++) {
                JSONObject p = products.getJSONObject(i);
                if (!p.has("id")) p.put("id", id());
                if (!p.has("category")) p.put("category", "Boissons");
                if (!p.has("variant")) p.put("variant", "");
                if (!p.has("priceType")) p.put("priceType", "fixed");
                if (!p.has("active")) p.put("active", true);
                if (!p.has("stock")) p.put("stock", 0);
                if (!p.has("image")) p.put("image", "");
            }
            if (prefs.getInt("schema_version", 1) < 2) migrateLegacyDebts();
            saveAll();
        } catch (Exception ignored) { saveAll(); }
    }

    private void migrateLegacyDebts() throws JSONException {
        for (int i = 0; i < debts.length(); i++) {
            JSONObject old = debts.getJSONObject(i);
            String clientId = findOrCreateClient(old.optString("customer", "Client"), "", "Importé depuis les anciennes dettes");
            long amount = old.optLong("amount", 0);
            JSONArray lines = new JSONArray().put(new JSONObject().put("productId", "legacy")
                .put("name", "Ancienne dette").put("variant", "").put("qty", 1)
                .put("unitPrice", amount).put("subtotal", amount).put("priceType", "free"));
            loans.put(new JSONObject().put("id", "legacy-debt-" + i).put("clientId", clientId)
                .put("day", old.optString("day", today())).put("date", old.optString("date", now()))
                .put("timestamp", timestampFromDay(old.optString("day", today())))
                .put("lines", lines).put("total", amount));
            if (old.optBoolean("paid", false)) debtPayments.put(new JSONObject()
                .put("id", "legacy-payment-" + i).put("clientId", clientId).put("amount", amount)
                .put("before", amount).put("after", 0).put("day", old.optString("day", today()))
                .put("date", old.optString("paidDate", old.optString("date", now())))
                .put("timestamp", timestampFromDay(old.optString("day", today()))));
        }
    }

    // Produits / Entana
    public JSONObject newProduct(String name, String category, String variant, long price, String priceType, boolean active, String image) throws JSONException {
        return new JSONObject().put("id", id()).put("name", name).put("category", category)
            .put("variant", variant).put("price", price).put("priceType", priceType)
            .put("active", active).put("stock", 0).put("image", image == null ? "" : image);
    }
    public void addProduct(String name, String category, String variant, long price, String priceType, boolean active, String image) throws JSONException {
        products.put(newProduct(name, category, variant, price, priceType, active, image)); saveAll();
    }
    public void addProduct(String name, long price, int stock) throws JSONException {
        JSONObject p = newProduct(name, "Boissons", "", price, "fixed", true, ""); p.put("stock", stock); products.put(p); saveAll();
    }
    public void updateProduct(int index, String name, String category, String variant, long price, String priceType, boolean active, String image) throws JSONException {
        JSONObject p = products.getJSONObject(index); p.put("name", name).put("category", category)
            .put("variant", variant).put("price", price).put("priceType", priceType).put("active", active)
            .put("image", image == null ? p.optString("image", "") : image); saveAll();
    }
    public void setProductActive(int index, boolean active) throws JSONException { products.getJSONObject(index).put("active", active); saveAll(); }
    public void deleteProduct(int index) { products = remove(products, index); saveAll(); }
    public void changeStock(int index, int delta) throws JSONException { JSONObject p = products.getJSONObject(index); p.put("stock", Math.max(0, p.optInt("stock") + delta)); saveAll(); }
    public JSONObject productById(String productId) { for (int i=0;i<products.length();i++){JSONObject p=products.optJSONObject(i);if(p!=null&&productId.equals(p.optString("id")))return p;}return null; }
    public JSONArray activeProducts() { JSONArray a=new JSONArray();for(int i=0;i<products.length();i++){JSONObject p=products.optJSONObject(i);if(p!=null&&p.optBoolean("active",true))a.put(p);}return a; }

    // Ventes / statistiques
    public JSONObject snapshotLine(JSONObject product, int qty, long unitPrice, long subtotal) throws JSONException {
        return new JSONObject().put("productId", product.optString("id", ""))
            .put("name", product.optString("name", "Produit")).put("variant", product.optString("variant", ""))
            .put("qty", qty).put("unitPrice", unitPrice).put("subtotal", subtotal)
            .put("priceType", product.optString("priceType", "fixed"));
    }
    public void addSale(JSONArray lines) throws JSONException { long total=sumLines(lines);if(total<=0)throw new JSONException("Montant invalide");for(int i=0;i<lines.length();i++){JSONObject line=lines.getJSONObject(i);JSONObject product=productById(line.optString("productId"));if(product!=null&&product.optInt("stock")>0){int quantity=line.optInt("qty",1);if(quantity>product.optInt("stock"))throw new JSONException("Stock insuffisant");product.put("stock",product.optInt("stock")-quantity);}}sales.put(new JSONObject().put("id",id()).put("date",now()).put("day",today()).put("timestamp",System.currentTimeMillis()).put("total",total).put("lines",lines));saveAll(); }
    public boolean completeSale(int[] quantities) throws JSONException { JSONArray lines=new JSONArray();for(int i=0;i<quantities.length&&i<products.length();i++){if(quantities[i]>0){JSONObject p=products.getJSONObject(i);long price=p.optLong("price");lines.put(snapshotLine(p,quantities[i],price,price*quantities[i]));}}if(lines.length()==0)return false;addSale(lines);return true; }
    public long salesBetween(long start,long end){return sumBetween(sales,"total",start,end);}
    public int salesCountBetween(long start,long end){return countBetween(sales,start,end);}
    public JSONArray topProducts(long start,long end){
        JSONArray names=new JSONArray(),qtys=new JSONArray(),result=new JSONArray();
        for(int i=0;i<sales.length();i++){JSONObject s=sales.optJSONObject(i);if(s==null||!inside(itemTimestamp(s),start,end))continue;JSONArray ls=s.optJSONArray("lines");if(ls==null)continue;for(int j=0;j<ls.length();j++){JSONObject l=ls.optJSONObject(j);if(l==null)continue;String n=l.optString("name","Produit");int q=l.optInt("qty",1),found=-1;for(int k=0;k<names.length();k++)if(n.equals(names.optString(k)))found=k;if(found<0){names.put(n);qtys.put(q);}else try{qtys.put(found,qtys.optInt(found)+q);}catch(Exception ignored){}}}
        for(int n=0;n<names.length();n++){int best=-1;for(int i=0;i<names.length();i++){boolean used=false;for(int r=0;r<result.length();r++){JSONObject x=result.optJSONObject(r);if(x!=null&&names.optString(i).equals(x.optString("name")))used=true;}if(!used&&(best<0||qtys.optInt(i)>qtys.optInt(best)))best=i;}if(best>=0)try{result.put(new JSONObject().put("name",names.optString(best)).put("qty",qtys.optInt(best)));}catch(Exception ignored){}}
        return result;
    }
    public long[] dailySales(int days){long[] r=new long[days];long todayStart=dayStart(System.currentTimeMillis());for(int i=0;i<sales.length();i++){JSONObject s=sales.optJSONObject(i);if(s==null)continue;long age=(todayStart-dayStart(itemTimestamp(s)))/86400000L;if(age>=0&&age<days)r[days-1-(int)age]+=s.optLong("total");}return r;}

    // Fournisseurs / versements
    public void addSupplier(String name)throws JSONException{suppliers.put(new JSONObject().put("id",id()).put("name",name));saveAll();}
    public void addVersement(String supplierId,String supplierName,long amount,String day,String note)throws JSONException{versements.put(new JSONObject().put("id",id()).put("supplierId",supplierId).put("supplier",supplierName).put("amount",amount).put("day",day).put("date",displayDay(day)).put("timestamp",timestampFromDay(day)).put("note",note));saveAll();}
    public void updateVersement(int index,String supplierId,String supplierName,long amount,String day,String note)throws JSONException{versements.getJSONObject(index).put("supplierId",supplierId).put("supplier",supplierName).put("amount",amount).put("day",day).put("date",displayDay(day)).put("timestamp",timestampFromDay(day)).put("note",note);saveAll();}
    public void deleteVersement(int index){versements=remove(versements,index);saveAll();}
    public long versementsTotal(){return sumBetween(versements,"amount",Long.MIN_VALUE,Long.MAX_VALUE);}

    // Clients / Trosa
    public String findOrCreateClient(String name,String phone,String note)throws JSONException{for(int i=0;i<clients.length();i++){JSONObject c=clients.getJSONObject(i);if(name.trim().equalsIgnoreCase(c.optString("name").trim()))return c.getString("id");}String cid=id();clients.put(new JSONObject().put("id",cid).put("name",name).put("phone",phone).put("note",note).put("created",now()));return cid;}
    public void addClient(String name,String phone,String note)throws JSONException{findOrCreateClient(name,phone,note);saveAll();}
    public void updateClient(int index,String name,String phone,String note)throws JSONException{clients.getJSONObject(index).put("name",name).put("phone",phone).put("note",note);saveAll();}
    public void addLoan(String clientId,JSONArray lines,String day)throws JSONException{long total=sumLines(lines);if(total<=0)throw new JSONException("Emprunt vide");loans.put(new JSONObject().put("id",id()).put("clientId",clientId).put("day",day).put("date",displayDay(day)+new SimpleDateFormat(" HH:mm",Locale.FRANCE).format(new Date())).put("timestamp",System.currentTimeMillis()).put("lines",lines).put("total",total));saveAll();}
    public void updateLoan(int index,JSONArray lines,String day)throws JSONException{long total=sumLines(lines);if(total<=0)throw new JSONException("Emprunt vide");JSONObject loan=loans.getJSONObject(index);loan.put("day",day).put("date",displayDay(day)+new SimpleDateFormat(" HH:mm",Locale.FRANCE).format(new Date())).put("lines",lines).put("total",total);saveAll();}
    public long clientBorrowed(String cid){long t=0;for(int i=0;i<loans.length();i++){JSONObject x=loans.optJSONObject(i);if(x!=null&&cid.equals(x.optString("clientId")))t+=x.optLong("total");}return t;}
    public long clientPaid(String cid){long t=0;for(int i=0;i<debtPayments.length();i++){JSONObject x=debtPayments.optJSONObject(i);if(x!=null&&cid.equals(x.optString("clientId")))t+=x.optLong("amount");}return t;}
    public long clientBalance(String cid){return Math.max(0,clientBorrowed(cid)-clientPaid(cid));}
    public void addDebtPayment(String cid,long amount)throws JSONException{long before=clientBalance(cid);if(amount<=0||amount>before)throw new JSONException("Paiement supérieur à la dette");debtPayments.put(new JSONObject().put("id",id()).put("clientId",cid).put("amount",amount).put("before",before).put("after",before-amount).put("day",today()).put("date",now()).put("timestamp",System.currentTimeMillis()));saveAll();}
    public void updateDebtPayment(int index,long amount)throws JSONException{JSONObject payment=debtPayments.getJSONObject(index);String cid=payment.getString("clientId");long available=clientBalance(cid)+payment.optLong("amount");if(amount<=0||amount>available)throw new JSONException("Paiement supérieur à la dette");payment.put("amount",amount).put("before",available).put("after",available-amount).put("date",now()).put("day",today()).put("timestamp",System.currentTimeMillis());saveAll();}
    public void deleteLoan(int index){loans=remove(loans,index);saveAll();}
    public void deleteDebtPayment(int index){debtPayments=remove(debtPayments,index);saveAll();}
    public String clientLastOperation(String cid){long latest=0;String date="Aucune opération";for(int i=0;i<loans.length();i++){JSONObject x=loans.optJSONObject(i);if(x!=null&&cid.equals(x.optString("clientId"))&&itemTimestamp(x)>=latest){latest=itemTimestamp(x);date=x.optString("date","");}}for(int i=0;i<debtPayments.length();i++){JSONObject x=debtPayments.optJSONObject(i);if(x!=null&&cid.equals(x.optString("clientId"))&&itemTimestamp(x)>=latest){latest=itemTimestamp(x);date=x.optString("date","");}}return date;}

    // Calculatrice
    public void addCalculation(String expression,String result)throws JSONException{calculatorHistory.put(new JSONObject().put("expression",expression).put("result",result).put("date",now()));while(calculatorHistory.length()>20)calculatorHistory=remove(calculatorHistory,0);saveAll();}

    // Utilitaires
    public static long startOfToday(){return dayStart(System.currentTimeMillis());}
    public static long endOfToday(){return startOfToday()+86400000L-1;}
    public static long startOfWeek(){Calendar c=Calendar.getInstance();c.setFirstDayOfWeek(Calendar.MONDAY);c.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);return dayStart(c.getTimeInMillis());}
    public static long startOfMonth(){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);return dayStart(c.getTimeInMillis());}
    public static String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.FRANCE).format(new Date());}
    public static String now(){return new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.FRANCE).format(new Date());}
    public static String displayDay(String day){try{return new SimpleDateFormat("dd/MM/yyyy",Locale.FRANCE).format(new SimpleDateFormat("yyyy-MM-dd",Locale.FRANCE).parse(day));}catch(Exception e){return day;}}
    private static String id(){return UUID.randomUUID().toString();}
    private static long sumLines(JSONArray lines)throws JSONException{long t=0;for(int i=0;i<lines.length();i++)t+=lines.getJSONObject(i).getLong("subtotal");return t;}
    private long sumBetween(JSONArray a,String field,long start,long end){long t=0;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&inside(itemTimestamp(x),start,end))t+=x.optLong(field);}return t;}
    private int countBetween(JSONArray a,long start,long end){int n=0;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&inside(itemTimestamp(x),start,end))n++;}return n;}
    private static boolean inside(long t,long start,long end){return t>=start&&t<=end;}
    private static long dayStart(long t){Calendar c=Calendar.getInstance();c.setTimeInMillis(t);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis();}
    private static long itemTimestamp(JSONObject x){return x.has("timestamp")?x.optLong("timestamp"):timestampFromDay(x.optString("day",today()));}
    private static long timestampFromDay(String day){try{return new SimpleDateFormat("yyyy-MM-dd",Locale.FRANCE).parse(day).getTime();}catch(Exception e){return System.currentTimeMillis();}}
    private static JSONArray remove(JSONArray a,int index){JSONArray r=new JSONArray();for(int i=0;i<a.length();i++)if(i!=index)r.put(a.opt(i));return r;}
}

package mg.barvolatiana.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(237, 246, 252);
    private static final int SURFACE = Color.rgb(250, 253, 255);
    private static final int WHITE = Color.WHITE;
    private static final int TURQUOISE = Color.rgb(20, 199, 172);
    private static final int TURQUOISE_DARK = Color.rgb(9, 156, 137);
    private static final int INK = Color.rgb(28, 35, 43);
    private static final int MUTED = Color.rgb(126, 139, 151);
    private static final int LINE = Color.rgb(222, 232, 239);
    private static final int RED = Color.rgb(226, 89, 89);

    private BarStore store;
    private FrameLayout content;
    private LinearLayout nav;
    private int page = 0;
    private long periodStart = BarStore.startOfToday(), periodEnd = BarStore.endOfToday();
    private String periodLabel = "Aujourd’hui";
    private JSONArray draftLines = new JSONArray();
    private String draftMode = "sale", draftClientId = "", draftDay = BarStore.today();
    private int draftEditIndex = -1;
    private TextView calculatorDisplay;
    private double calcAccumulator = 0;
    private String calcOperator = "", calcExpression = "";
    private boolean calcNewInput = true;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new BarStore(this);
        buildShell();
        showPage(0);
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(15), dp(16), dp(13));
        header.setBackgroundColor(SURFACE);
        LinearLayout names = new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL);
        names.addView(text("Bar Volatiana", 22, INK, true));
        names.addView(text(capitalize(new SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(new Date())), 13, MUTED, false));
        header.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = text("BV", 14, WHITE, true); badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(TURQUOISE, 18, Color.TRANSPARENT, 0));
        header.addView(badge, new LinearLayout.LayoutParams(dp(40), dp(40)));
        root.addView(header);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER); nav.setPadding(dp(3), dp(5), dp(3), dp(6));
        nav.setBackgroundColor(WHITE);
        final String[] labels = {"⌂\nAccueil", "↓\nVersements", "T\nTrosa", "□\nEntana", "=\nCalculatrice"};
        for (int i=0;i<labels.length;i++) {
            final int target=i;
            TextView item=text(labels[i], 11, MUTED, false); item.setGravity(Gravity.CENTER); item.setLineSpacing(0, .95f);
            item.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showPage(target);}});
            nav.addView(item,new LinearLayout.LayoutParams(0,dp(56),1));
        }
        root.addView(nav);
        setContentView(root);
    }

    private void showPage(int target) {
        page=target; content.removeAllViews();
        for(int i=0;i<nav.getChildCount();i++){
            TextView item=(TextView)nav.getChildAt(i); item.setTextColor(i==page?TURQUOISE_DARK:MUTED);
            item.setTypeface(null,i==page?Typeface.BOLD:Typeface.NORMAL);
        }
        if(page==0)home(); else if(page==1)versements(""); else if(page==2)trosa(""); else if(page==3)entana("","all"); else calculator();
    }

    private LinearLayout screen(String title,String subtitle){
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(16),dp(18),dp(16),dp(28));
        body.addView(text(title,26,INK,true));
        TextView sub=text(subtitle,14,MUTED,false); sub.setPadding(0,dp(3),0,dp(14)); body.addView(sub);
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2)); content.addView(scroll,new FrameLayout.LayoutParams(-1,-1)); return body;
    }

    // ACCUEIL + VENTES
    private void home(){
        LinearLayout body=screen("Vue d’ensemble", "Statistiques réelles des ventes enregistrées.");
        LinearLayout filters=new LinearLayout(this); filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.addView(filterButton("Jour",0),new LinearLayout.LayoutParams(0,dp(42),1));
        filters.addView(filterButton("Semaine",1),new LinearLayout.LayoutParams(0,dp(42),1));
        filters.addView(filterButton("Mois",2),new LinearLayout.LayoutParams(0,dp(42),1));
        filters.addView(filterButton("Période",3),new LinearLayout.LayoutParams(0,dp(42),1)); body.addView(filters);
        TextView current=text(periodLabel,13,TURQUOISE_DARK,true); current.setPadding(dp(4),dp(10),0,dp(8)); body.addView(current);

        long all=store.salesBetween(Long.MIN_VALUE,Long.MAX_VALUE), selected=store.salesBetween(periodStart,periodEnd);
        body.addView(bigStat("CHIFFRE D’AFFAIRES TOTAL",money(all),TURQUOISE));
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(smallStat("Ventes",money(selected)),weighted(0,dp(6)));
        stats.addView(smallStat("Nombre",String.valueOf(store.salesCountBetween(periodStart,periodEnd))),weighted(dp(6),0));body.addView(stats);

        TextView graphTitle=section("Évolution sur 7 jours");body.addView(graphTitle);
        SalesChart chart=new SalesChart(); chart.setBackground(round(WHITE,20,Color.TRANSPARENT,0));body.addView(chart,new LinearLayout.LayoutParams(-1,dp(170)));

        body.addView(section("Produits les plus vendus"));
        JSONArray top=store.topProducts(periodStart,periodEnd);
        if(top.length()==0)body.addView(empty("Aucune vente pour cette période."));
        for(int i=0;i<Math.min(5,top.length());i++){JSONObject x=top.optJSONObject(i);if(x!=null)body.addView(simpleRow((i+1)+".  "+x.optString("name"),x.optInt("qty")+" vendu(s)"));}

        Button sale=primary("+  Nouvelle vente"); sale.setOnClickListener(new View.OnClickListener(){public void onClick(View v){startDraft("sale","");}});
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(56));sp.setMargins(0,dp(20),0,0);body.addView(sale,sp);
    }

    private Button filterButton(String label,final int mode){
        Button b=flatButton(label);b.setTextSize(12);b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            if(mode==0){periodStart=BarStore.startOfToday();periodEnd=BarStore.endOfToday();periodLabel="Aujourd’hui";home();}
            else if(mode==1){periodStart=BarStore.startOfWeek();periodEnd=Long.MAX_VALUE;periodLabel="Cette semaine";home();}
            else if(mode==2){periodStart=BarStore.startOfMonth();periodEnd=Long.MAX_VALUE;periodLabel="Ce mois";home();}
            else chooseCustomPeriod();
        }});return b;
    }

    private void chooseCustomPeriod(){chooseDate("Date de début",BarStore.today(),new DateResult(){public void selected(final String start){chooseDate("Date de fin",start,new DateResult(){public void selected(String end){periodStart=parseDay(start);periodEnd=parseDay(end)+86399999L;periodLabel=BarStore.displayDay(start)+" — "+BarStore.displayDay(end);home();}});}});}

    private void startDraft(String mode,String clientId){draftMode=mode;draftClientId=clientId;draftDay=BarStore.today();draftLines=new JSONArray();draftEditIndex=-1;operationPage();}
    private void editLoanDraft(int index){JSONObject loan=store.loans.optJSONObject(index);if(loan==null)return;draftMode="loan-edit";draftClientId=loan.optString("clientId");draftDay=loan.optString("day",BarStore.today());draftLines=loan.optJSONArray("lines");if(draftLines==null)draftLines=new JSONArray();draftEditIndex=index;operationPage();}
    private void operationPage(){
        content.removeAllViews();LinearLayout body=screen(draftMode.equals("sale")?"Nouvelle vente":(draftMode.equals("loan-edit")?"Modifier l’emprunt":"Nouvel emprunt"),draftMode.equals("sale")?"Ajoutez tous les produits vendus.":"Les prix sont copiés et resteront inchangés dans l’historique.");
        Button add=primary("+  Ajouter un produit");add.setOnClickListener(new View.OnClickListener(){public void onClick(View v){lineDialog();}});body.addView(add);
        long total=0;for(int i=0;i<draftLines.length();i++){JSONObject l=draftLines.optJSONObject(i);if(l==null)continue;total+=l.optLong("subtotal");String variant=l.optString("variant");body.addView(simpleRow(l.optInt("qty")+" × "+l.optString("name")+(variant.length()>0?" "+variant:""),money(l.optLong("subtotal"))));}
        TextView sum=text("Total  "+money(total),24,INK,true);sum.setGravity(Gravity.CENTER);sum.setPadding(dp(12),dp(18),dp(12),dp(18));sum.setBackground(round(Color.rgb(216,248,243),18,Color.TRANSPARENT,0));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.setMargins(0,dp(14),0,0);body.addView(sum,tp);
        Button save=primary(draftMode.equals("sale")?"Enregistrer la vente":(draftMode.equals("loan-edit")?"Enregistrer la modification":"Enregistrer l’emprunt"));save.setEnabled(total>0);save.setAlpha(total>0?1f:.4f);final long finalTotal=total;save.setOnClickListener(new View.OnClickListener(){public void onClick(View v){if(finalTotal<=0)return;if(draftMode.equals("loan-edit")){confirm("Modifier cet emprunt ?","Les anciens détails seront remplacés par cette nouvelle version.",new Runnable(){public void run(){try{store.updateLoan(draftEditIndex,draftLines,draftDay);toast("Emprunt modifié");clientDetail(draftClientId);}catch(Exception e){toast("Modification impossible");}}});return;}try{if(draftMode.equals("sale")){store.addSale(draftLines);toast("Vente enregistrée");showPage(0);}else{store.addLoan(draftClientId,draftLines,draftDay);toast("Emprunt enregistré");clientDetail(draftClientId);}}catch(Exception e){toast("Impossible d’enregistrer l’opération");}}});LinearLayout.LayoutParams sv=new LinearLayout.LayoutParams(-1,dp(56));sv.setMargins(0,dp(12),0,0);body.addView(save,sv);
        Button cancel=outline("Annuler");cancel.setOnClickListener(new View.OnClickListener(){public void onClick(View v){if(draftMode.equals("sale"))showPage(0);else clientDetail(draftClientId);}});LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(52));cp.setMargins(0,dp(8),0,0);body.addView(cancel,cp);
    }

    private void lineDialog(){
        final JSONArray active=store.activeProducts();if(active.length()==0){toast("Ajoutez d’abord un produit dans Entana");return;}
        final Spinner products=new Spinner(this);List<String> names=new ArrayList<String>();for(int i=0;i<active.length();i++){JSONObject p=active.optJSONObject(i);names.add(productLabel(p));}products.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
        final EditText qty=input("Quantité",true);qty.setText("1");final EditText free=input("Montant total (seulement pour prix libre)",true);
        LinearLayout form=dialogForm(products,qty,free);
        new AlertDialog.Builder(this).setTitle("Ajouter un produit").setView(form).setNegativeButton("Annuler",null).setPositiveButton("Ajouter",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{JSONObject p=active.getJSONObject(products.getSelectedItemPosition());int q=Integer.parseInt(qty.getText().toString());long unit=p.optLong("price");long subtotal="free".equals(p.optString("priceType"))?Long.parseLong(free.getText().toString()):unit*q;if(q<=0||subtotal<=0)throw new Exception();if("free".equals(p.optString("priceType")))unit=subtotal/q;draftLines.put(store.snapshotLine(p,q,unit,subtotal));operationPage();}catch(Exception e){toast("Vérifiez la quantité et le montant");}}}).show();
    }

    // VERSEMENTS
    private void versements(final String query){
        LinearLayout body=screen("Versements","Paiements effectués aux fournisseurs.");body.addView(bigStat("TOTAL DES VERSEMENTS",money(store.versementsTotal()),TURQUOISE));
        Button add=primary("+  Nouveau versement");add.setOnClickListener(new View.OnClickListener(){public void onClick(View v){versementDialog(-1);}});LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(54));ap.setMargins(0,dp(12),0,dp(10));body.addView(add,ap);
        EditText search=input("Rechercher un fournisseur",false);search.setText(query);body.addView(search,new LinearLayout.LayoutParams(-1,dp(50)));search.addTextChangedListener(new SimpleWatcher(){public void afterTextChanged(Editable e){String q=e.toString();if(!q.equals(query))versements(q);}});
        int shown=0;for(int i=store.versements.length()-1;i>=0;i--){final int index=i;JSONObject x=store.versements.optJSONObject(i);if(x==null||!contains(x.optString("supplier")+" "+x.optString("date"),query))continue;shown++;LinearLayout card=infoCard();LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.addView(text(x.optString("supplier"),17,INK,true));info.addView(text(x.optString("date")+(x.optString("note").length()>0?" · "+x.optString("note"):""),13,MUTED,false));card.addView(info,new LinearLayout.LayoutParams(0,-2,1));card.addView(text(money(x.optLong("amount")),15,TURQUOISE_DARK,true));card.setOnClickListener(new View.OnClickListener(){public void onClick(View v){versementActions(index);}});body.addView(card);}
        if(shown==0)body.addView(empty("Aucun versement trouvé."));
    }

    private void versementActions(final int index){new AlertDialog.Builder(this).setTitle("Versement").setItems(new String[]{"Modifier","Supprimer"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){if(which==0)versementDialog(index);else confirm("Supprimer ce versement ?","Cette action est définitive.",new Runnable(){public void run(){store.deleteVersement(index);versements("");}});}}).show();}

    private void versementDialog(final int index){
        if(store.suppliers.length()==0){supplierDialog(new Runnable(){public void run(){versementDialog(index);}});return;}
        final Spinner supplier=new Spinner(this);List<String> names=new ArrayList<String>();for(int i=0;i<store.suppliers.length();i++)names.add(store.suppliers.optJSONObject(i).optString("name"));supplier.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
        final EditText amount=input("Montant en ariary",true),note=input("Note facultative",false);final Button date=flatButton("Date : "+BarStore.displayDay(BarStore.today()));final String[] day={BarStore.today()};date.setOnClickListener(new View.OnClickListener(){public void onClick(View v){chooseDate("Date du versement",day[0],new DateResult(){public void selected(String value){day[0]=value;date.setText("Date : "+BarStore.displayDay(value));}});}});
        if(index>=0){JSONObject old=store.versements.optJSONObject(index);amount.setText(String.valueOf(old.optLong("amount")));note.setText(old.optString("note"));day[0]=old.optString("day",BarStore.today());date.setText("Date : "+BarStore.displayDay(day[0]));for(int i=0;i<store.suppliers.length();i++)if(old.optString("supplierId").equals(store.suppliers.optJSONObject(i).optString("id")))supplier.setSelection(i);}
        Button newSupplier=outline("+ Ajouter un fournisseur");newSupplier.setOnClickListener(new View.OnClickListener(){public void onClick(View v){supplierDialog(new Runnable(){public void run(){versementDialog(index);}});}});
        LinearLayout form=dialogForm(supplier,amount,date,note,newSupplier);
        new AlertDialog.Builder(this).setTitle(index<0?"Nouveau versement":"Modifier le versement").setView(form).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{JSONObject s=store.suppliers.getJSONObject(supplier.getSelectedItemPosition());long value=Long.parseLong(amount.getText().toString());if(value<=0)throw new Exception();if(index<0)store.addVersement(s.getString("id"),s.getString("name"),value,day[0],note.getText().toString().trim());else store.updateVersement(index,s.getString("id"),s.getString("name"),value,day[0],note.getText().toString().trim());toast("Versement enregistré");versements("");}catch(Exception e){toast("Vérifiez le fournisseur et le montant");}}}).show();
    }

    private void supplierDialog(final Runnable after){final EditText name=input("Nom du fournisseur : Star, Dzama…",false);new AlertDialog.Builder(this).setTitle("Nouveau fournisseur").setView(dialogForm(name)).setNegativeButton("Annuler",null).setPositiveButton("Ajouter",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{String n=name.getText().toString().trim();if(n.length()==0)throw new Exception();store.addSupplier(n);toast("Fournisseur ajouté");if(after!=null)after.run();}catch(Exception e){toast("Saisissez un nom");}}}).show();}

    // TROSA
    private void trosa(final String query){
        LinearLayout body=screen("Trosa","Crédits et paiements des clients.");Button add=primary("+  Nouveau client");add.setOnClickListener(new View.OnClickListener(){public void onClick(View v){clientDialog(-1);}});body.addView(add);
        EditText search=input("Rechercher un client",false);search.setText(query);LinearLayout.LayoutParams se=new LinearLayout.LayoutParams(-1,dp(50));se.setMargins(0,dp(10),0,0);body.addView(search,se);search.addTextChangedListener(new SimpleWatcher(){public void afterTextChanged(Editable e){String q=e.toString();if(!q.equals(query))trosa(q);}});
        int shown=0;for(int i=0;i<store.clients.length();i++){JSONObject c=store.clients.optJSONObject(i);if(c==null||!contains(c.optString("name"),query))continue;shown++;final String cid=c.optString("id");long balance=store.clientBalance(cid);LinearLayout row=infoCard();LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.addView(text(c.optString("name"),17,INK,true));info.addView(text("Dernière opération : "+store.clientLastOperation(cid),12,MUTED,false));row.addView(info,new LinearLayout.LayoutParams(0,-2,1));TextView debt=text(money(balance),15,balance>0?RED:TURQUOISE_DARK,true);row.addView(debt);row.setOnClickListener(new View.OnClickListener(){public void onClick(View v){clientDetail(cid);}});body.addView(row);}
        if(shown==0)body.addView(empty("Aucun client trouvé."));
    }

    private void clientDialog(final int index){final EditText name=input("Nom du client",false),phone=input("Téléphone facultatif",false),note=input("Note facultative",false);if(index>=0){JSONObject c=store.clients.optJSONObject(index);name.setText(c.optString("name"));phone.setText(c.optString("phone"));note.setText(c.optString("note"));}new AlertDialog.Builder(this).setTitle(index<0?"Nouveau client":"Modifier le client").setView(dialogForm(name,phone,note)).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{String n=name.getText().toString().trim();if(n.length()==0)throw new Exception();if(index<0)store.addClient(n,phone.getText().toString(),note.getText().toString());else store.updateClient(index,n,phone.getText().toString(),note.getText().toString());toast("Client enregistré");trosa("");}catch(Exception e){toast("Saisissez le nom du client");}}}).show();}

    private void clientDetail(final String cid){
        content.removeAllViews();JSONObject client=null;int clientIndex=-1;for(int i=0;i<store.clients.length();i++){JSONObject c=store.clients.optJSONObject(i);if(c!=null&&cid.equals(c.optString("id"))){client=c;clientIndex=i;break;}}if(client==null){showPage(2);return;}
        LinearLayout body=screen(client.optString("name"),client.optString("phone").length()>0?client.optString("phone"):"Fiche du client");
        LinearLayout totals=new LinearLayout(this);totals.setOrientation(LinearLayout.HORIZONTAL);totals.addView(smallStat("Emprunts",money(store.clientBorrowed(cid))),weighted(0,dp(4)));totals.addView(smallStat("Paiements",money(store.clientPaid(cid))),weighted(dp(4),dp(4)));totals.addView(smallStat("Reste",money(store.clientBalance(cid))),weighted(dp(4),0));body.addView(totals);
        Button loan=primary("+ Ajouter un emprunt");loan.setOnClickListener(new View.OnClickListener(){public void onClick(View v){startDraft("loan",cid);}});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(14),0,0);body.addView(loan,lp);
        Button pay=outline("Enregistrer un paiement");pay.setOnClickListener(new View.OnClickListener(){public void onClick(View v){paymentDialog(cid);}});LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(52));pp.setMargins(0,dp(8),0,0);body.addView(pay,pp);
        body.addView(section("Historique"));boolean found=false;
        for(int i=store.loans.length()-1;i>=0;i--){JSONObject x=store.loans.optJSONObject(i);if(x==null||!cid.equals(x.optString("clientId")))continue;found=true;final int idx=i;LinearLayout row=infoCard();LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.addView(text("Emprunt · "+x.optString("date"),15,INK,true));JSONArray lines=x.optJSONArray("lines");info.addView(text(linesSummary(lines),12,MUTED,false));row.addView(info,new LinearLayout.LayoutParams(0,-2,1));row.addView(text("+ "+money(x.optLong("total")),14,RED,true));row.setOnClickListener(new View.OnClickListener(){public void onClick(View v){new AlertDialog.Builder(MainActivity.this).setTitle("Emprunt").setItems(new String[]{"Modifier","Supprimer"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){if(which==0)editLoanDraft(idx);else confirm("Supprimer cet emprunt ?","Le solde du client sera recalculé.",new Runnable(){public void run(){store.deleteLoan(idx);clientDetail(cid);}});}}).show();}});body.addView(row);}
        for(int i=store.debtPayments.length()-1;i>=0;i--){JSONObject x=store.debtPayments.optJSONObject(i);if(x==null||!cid.equals(x.optString("clientId")))continue;found=true;final int idx=i;LinearLayout row=infoCard();LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.addView(text("Paiement · "+x.optString("date"),15,INK,true));info.addView(text("Avant "+money(x.optLong("before"))+" · Reste "+money(x.optLong("after")),12,MUTED,false));row.addView(info,new LinearLayout.LayoutParams(0,-2,1));row.addView(text("− "+money(x.optLong("amount")),14,TURQUOISE_DARK,true));row.setOnClickListener(new View.OnClickListener(){public void onClick(View v){new AlertDialog.Builder(MainActivity.this).setTitle("Paiement").setItems(new String[]{"Modifier","Supprimer"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){if(which==0)paymentDialog(cid,idx);else confirm("Supprimer ce paiement ?","Le solde du client sera recalculé.",new Runnable(){public void run(){store.deleteDebtPayment(idx);clientDetail(cid);}});}}).show();}});body.addView(row);}
        if(!found)body.addView(empty("Aucune opération pour ce client."));
        final int editIndex=clientIndex;Button edit=flatButton("Modifier le client");edit.setOnClickListener(new View.OnClickListener(){public void onClick(View v){clientDialog(editIndex);}});body.addView(edit,new LinearLayout.LayoutParams(-1,dp(48)));
    }

    private void paymentDialog(final String cid){paymentDialog(cid,-1);}
    private void paymentDialog(final String cid,final int editIndex){final long balance=store.clientBalance(cid)+(editIndex>=0?store.debtPayments.optJSONObject(editIndex).optLong("amount"):0);if(balance<=0){toast("Ce client n’a aucune dette");return;}final EditText amount=input("Montant payé · maximum "+money(balance),true);if(editIndex>=0)amount.setText(String.valueOf(store.debtPayments.optJSONObject(editIndex).optLong("amount")));new AlertDialog.Builder(this).setTitle(editIndex<0?"Enregistrer un paiement":"Modifier le paiement").setMessage("Montant maximum : "+money(balance)).setView(dialogForm(amount)).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{final long a=Long.parseLong(amount.getText().toString());if(editIndex<0){store.addDebtPayment(cid,a);toast("Paiement enregistré · reste "+money(store.clientBalance(cid)));clientDetail(cid);}else confirm("Modifier ce paiement ?","La dette restante sera recalculée.",new Runnable(){public void run(){try{store.updateDebtPayment(editIndex,a);toast("Paiement modifié");clientDetail(cid);}catch(Exception e){toast("Montant supérieur à la dette disponible");}}});}catch(Exception e){toast("Le paiement doit être positif et inférieur à la dette");}}}).show();}

    // ENTANA
    private void entana(final String query,final String filter){
        LinearLayout body=screen("Entana","Produits proposés dans les ventes et Trosa.");Button add=primary("+  Nouveau produit");add.setOnClickListener(new View.OnClickListener(){public void onClick(View v){productDialog(-1);}});body.addView(add);
        EditText search=input("Rechercher un produit",false);search.setText(query);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(50));sp.setMargins(0,dp(10),0,0);body.addView(search,sp);search.addTextChangedListener(new SimpleWatcher(){public void afterTextChanged(Editable e){String q=e.toString();if(!q.equals(query))entana(q,filter);}});
        LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.HORIZONTAL);filters.addView(productFilter("Tous","all",filter,query),new LinearLayout.LayoutParams(0,dp(42),1));filters.addView(productFilter("Actifs","active",filter,query),new LinearLayout.LayoutParams(0,dp(42),1));filters.addView(productFilter("Inactifs","inactive",filter,query),new LinearLayout.LayoutParams(0,dp(42),1));body.addView(filters);
        int shown=0;for(int i=0;i<store.products.length();i++){final int index=i;JSONObject p=store.products.optJSONObject(i);if(p==null||!contains(productLabel(p),query))continue;boolean active=p.optBoolean("active",true);if("active".equals(filter)&&!active||"inactive".equals(filter)&&active)continue;shown++;LinearLayout row=infoCard();TextView initial=text(p.optString("name","?").substring(0,1).toUpperCase(Locale.FRANCE),16,WHITE,true);initial.setGravity(Gravity.CENTER);initial.setBackground(round(active?TURQUOISE:MUTED,22,Color.TRANSPARENT,0));row.addView(initial,new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,0,0);info.addView(text(productLabel(p),16,INK,true));info.addView(text(p.optString("category")+" · "+("free".equals(p.optString("priceType"))?"montant libre":money(p.optLong("price")))+" · stock "+p.optInt("stock")+(active?"":" · inactif"),12,active?MUTED:RED,false));row.addView(info,new LinearLayout.LayoutParams(0,-2,1));row.setOnClickListener(new View.OnClickListener(){public void onClick(View v){productActions(index);}});body.addView(row);}
        if(shown==0)body.addView(empty("Aucun produit trouvé."));
    }

    private Button productFilter(String title,final String value,String selected,final String query){Button b=flatButton(title);if(value.equals(selected))b.setTextColor(TURQUOISE_DARK);b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){entana(query,value);}});return b;}
    private void productActions(final int index){final JSONObject p=store.products.optJSONObject(index);String toggle=p.optBoolean("active",true)?"Désactiver":"Réactiver";new AlertDialog.Builder(this).setTitle(productLabel(p)).setItems(new String[]{"Modifier","Ajuster le stock",toggle,"Supprimer"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){if(which==0)productDialog(index);else if(which==1)stockDialog(index);else if(which==2){try{store.setProductActive(index,!p.optBoolean("active",true));entana("","all");}catch(Exception e){toast("Modification impossible");}}else confirm("Supprimer ce produit ?","Il restera présent dans les anciens historiques.",new Runnable(){public void run(){store.deleteProduct(index);entana("","all");}});}}).show();}

    private void productDialog(final int index){
        final EditText name=input("Nom",false),category=input("Catégorie",false),variant=input("Format : 65 cl, 33 cl…",false),price=input("Prix en ariary",true),stock=input("Quantité en stock",true),image=input("Image facultative (chemin)",false);stock.setText("0");final CheckBox free=new CheckBox(this);free.setText("Montant libre lors de l’opération");free.setTextColor(INK);final CheckBox active=new CheckBox(this);active.setText("Produit actif");active.setTextColor(INK);active.setChecked(true);
        if(index>=0){JSONObject p=store.products.optJSONObject(index);name.setText(p.optString("name"));category.setText(p.optString("category"));variant.setText(p.optString("variant"));price.setText(String.valueOf(p.optLong("price")));stock.setText(String.valueOf(p.optInt("stock")));image.setText(p.optString("image"));free.setChecked("free".equals(p.optString("priceType")));active.setChecked(p.optBoolean("active",true));}
        new AlertDialog.Builder(this).setTitle(index<0?"Nouveau produit":"Modifier le produit").setView(dialogForm(name,category,variant,price,stock,free,active,image)).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{String n=name.getText().toString().trim(),cat=category.getText().toString().trim(),type=free.isChecked()?"free":"fixed";long amount=price.getText().toString().trim().length()==0?0:Long.parseLong(price.getText().toString());int stockValue=Integer.parseInt(stock.getText().toString());if(n.length()==0||cat.length()==0||stockValue<0||(!free.isChecked()&&amount<=0))throw new Exception();if(index<0){store.addProduct(n,cat,variant.getText().toString().trim(),amount,type,active.isChecked(),image.getText().toString().trim());store.products.getJSONObject(store.products.length()-1).put("stock",stockValue);}else{store.updateProduct(index,n,cat,variant.getText().toString().trim(),amount,type,active.isChecked(),image.getText().toString().trim());store.products.getJSONObject(index).put("stock",stockValue);}store.saveAll();toast("Produit enregistré");entana("","all");}catch(Exception e){toast("Vérifiez le nom, la catégorie, le prix et le stock");}}}).show();
    }

    private void stockDialog(final int index){final JSONObject p=store.products.optJSONObject(index);final EditText value=input("Nouvelle quantité",true);value.setText(String.valueOf(p.optInt("stock")));new AlertDialog.Builder(this).setTitle("Stock · "+productLabel(p)).setView(dialogForm(value)).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{int target=Integer.parseInt(value.getText().toString());if(target<0)throw new Exception();store.changeStock(index,target-p.optInt("stock"));toast("Stock mis à jour");entana("","all");}catch(Exception e){toast("Quantité invalide");}}}).show();}

    // CALCULATRICE — toujours dernier onglet
    private void calculator(){
        LinearLayout body=screen("Calculatrice","Calculs rapides en ariary.");calculatorDisplay=text("0",38,INK,true);calculatorDisplay.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);calculatorDisplay.setPadding(dp(18),dp(8),dp(18),dp(8));calculatorDisplay.setBackground(round(WHITE,20,Color.TRANSPARENT,0));body.addView(calculatorDisplay,new LinearLayout.LayoutParams(-1,dp(92)));
        String[][] keys={{"C","±","%","⌫"},{"7","8","9","÷"},{"4","5","6","×"},{"1","2","3","−"},{"0",".","=","+"}};
        for(int r=0;r<keys.length;r++){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);for(int c=0;c<4;c++){final String key=keys[r][c];Button b=calcButton(key);b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){calculatorKey(key);}});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(60),1);p.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(b,p);}body.addView(row);}
        body.addView(section("Historique récent"));if(store.calculatorHistory.length()==0)body.addView(empty("Aucun calcul récent."));for(int i=store.calculatorHistory.length()-1;i>=Math.max(0,store.calculatorHistory.length()-8);i--){JSONObject x=store.calculatorHistory.optJSONObject(i);if(x!=null)body.addView(simpleRow(x.optString("expression"),x.optString("result")));}
    }

    private void calculatorKey(String key){
        String shown=calculatorDisplay.getText().toString();
        if(key.matches("[0-9]")){calculatorDisplay.setText(calcNewInput?key:("0".equals(shown)?key:shown+key));calcNewInput=false;}
        else if(".".equals(key)){if(calcNewInput){calculatorDisplay.setText("0.");calcNewInput=false;}else if(!shown.contains("."))calculatorDisplay.setText(shown+".");}
        else if("C".equals(key)){calculatorDisplay.setText("0");calcAccumulator=0;calcOperator="";calcExpression="";calcNewInput=true;}
        else if("⌫".equals(key)){if(!calcNewInput&&shown.length()>1)calculatorDisplay.setText(shown.substring(0,shown.length()-1));else calculatorDisplay.setText("0");}
        else if("±".equals(key)){double v=parseNumber(shown);calculatorDisplay.setText(formatNumber(-v));}
        else if("%".equals(key)){double v=parseNumber(shown)/100d;calculatorDisplay.setText(formatNumber(v));calcNewInput=true;}
        else if("+".equals(key)||"−".equals(key)||"×".equals(key)||"÷".equals(key)){double current=parseNumber(shown);if(calcOperator.length()>0&&!calcNewInput)current=apply(calcAccumulator,current,calcOperator);calcAccumulator=current;calcOperator=key;calcExpression=formatNumber(current)+" "+key+" ";calculatorDisplay.setText(formatNumber(current));calcNewInput=true;}
        else if("=".equals(key)&&calcOperator.length()>0){double right=parseNumber(shown),result=apply(calcAccumulator,right,calcOperator);String expression=calcExpression+formatNumber(right);calculatorDisplay.setText(formatNumber(result));try{store.addCalculation(expression,formatNumber(result));}catch(Exception ignored){}calcAccumulator=result;calcOperator="";calcExpression="";calcNewInput=true;}
    }

    private double apply(double a,double b,String op){if("+".equals(op))return a+b;if("−".equals(op))return a-b;if("×".equals(op))return a*b;if("÷".equals(op))return b==0?0:a/b;return b;}

    // COMPOSANTS ET UTILITAIRES
    private LinearLayout bigStat(String label,String value,int color){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(16),dp(18),dp(16));box.setBackground(round(color,20,Color.TRANSPARENT,0));box.addView(text(label,12,WHITE,true));TextView amount=text(value,29,WHITE,true);amount.setPadding(0,dp(5),0,0);box.addView(amount);return box;}
    private LinearLayout smallStat(String label,String value){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(13),dp(13),dp(13),dp(13));box.setBackground(round(WHITE,18,Color.TRANSPARENT,0));box.addView(text(label,12,MUTED,false));TextView v=text(value,17,INK,true);v.setPadding(0,dp(4),0,0);box.addView(v);return box;}
    private LinearLayout.LayoutParams weighted(int left,int right){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(left,dp(10),right,0);return p;}
    private TextView section(String label){TextView t=text(label,18,INK,true);t.setPadding(0,dp(22),0,dp(9));return t;}
    private LinearLayout infoCard(){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(14),dp(13),dp(14),dp(13));row.setBackground(round(WHITE,18,Color.TRANSPARENT,0));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(9),0,0);row.setLayoutParams(p);row.setElevation(dp(2));return row;}
    private LinearLayout simpleRow(String title,String right){LinearLayout row=infoCard();row.addView(text(title,15,INK,true),new LinearLayout.LayoutParams(0,-2,1));row.addView(text(right,13,TURQUOISE_DARK,true));return row;}
    private TextView empty(String value){TextView t=text(value,14,MUTED,false);t.setGravity(Gravity.CENTER);t.setPadding(dp(16),dp(24),dp(16),dp(24));return t;}
    private Button primary(String label){Button b=new Button(this);b.setText(label);b.setTextSize(15);b.setTypeface(null,Typeface.BOLD);b.setTextColor(WHITE);b.setAllCaps(false);b.setBackground(round(TURQUOISE,18,Color.TRANSPARENT,0));return b;}
    private Button outline(String label){Button b=primary(label);b.setTextColor(TURQUOISE_DARK);b.setBackground(round(WHITE,16,TURQUOISE,1));return b;}
    private Button flatButton(String label){Button b=new Button(this);b.setText(label);b.setTextSize(13);b.setTextColor(INK);b.setAllCaps(false);b.setBackground(round(Color.rgb(225,244,248),14,Color.TRANSPARENT,0));return b;}
    private Button calcButton(String label){Button b=flatButton(label);b.setTextSize(20);if("+−×÷=".contains(label)){b.setTextColor(WHITE);b.setBackground(round(TURQUOISE,18,Color.TRANSPARENT,0));}else b.setBackground(round(WHITE,18,Color.TRANSPARENT,0));return b;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0,1.08f);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private EditText input(String hint,boolean numeric){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(15);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setSingleLine(true);e.setPadding(dp(13),dp(8),dp(13),dp(8));e.setInputType(numeric?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);e.setBackground(round(WHITE,14,LINE,1));return e;}
    private LinearLayout dialogForm(View...views){LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(20),dp(8),dp(20),0);for(View v:views){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(7),0,0);form.addView(v,p);}return form;}
    private GradientDrawable round(int fill,int radius,int stroke,int width){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(width>0)d.setStroke(dp(width),stroke);return d;}
    private void confirm(String title,String message,final Runnable yes){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Annuler",null).setPositiveButton("Confirmer",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){yes.run();}}).show();}
    private void chooseDate(String title,String initial,final DateResult callback){Calendar c=Calendar.getInstance();try{c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.FRANCE).parse(initial));}catch(Exception ignored){}new DatePickerDialog(this,new DatePickerDialog.OnDateSetListener(){public void onDateSet(android.widget.DatePicker v,int y,int m,int d){callback.selected(String.format(Locale.FRANCE,"%04d-%02d-%02d",y,m+1,d));}},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}
    private String linesSummary(JSONArray lines){if(lines==null)return "";StringBuilder s=new StringBuilder();for(int i=0;i<lines.length();i++){JSONObject l=lines.optJSONObject(i);if(l==null)continue;if(s.length()>0)s.append(" · ");s.append(l.optInt("qty")).append("× ").append(l.optString("name"));}return s.toString();}
    private String productLabel(JSONObject p){if(p==null)return "Produit";String v=p.optString("variant");return p.optString("name")+(v.length()>0?" "+v:"");}
    private boolean contains(String value,String query){return query==null||query.trim().length()==0||value.toLowerCase(Locale.FRANCE).contains(query.trim().toLowerCase(Locale.FRANCE));}
    private String money(long value){return NumberFormat.getIntegerInstance(Locale.FRANCE).format(value).replace('\u00a0',' ')+" Ar";}
    private double parseNumber(String value){try{return Double.parseDouble(value.replace(',','.'));}catch(Exception e){return 0;}}
    private String formatNumber(double value){if(Math.abs(value-Math.rint(value))<0.0000001)return String.valueOf((long)Math.rint(value));return String.format(Locale.FRANCE,"%.2f",value);}
    private long parseDay(String day){try{return new SimpleDateFormat("yyyy-MM-dd",Locale.FRANCE).parse(day).getTime();}catch(Exception e){return System.currentTimeMillis();}}
    private String capitalize(String s){return s.length()==0?s:s.substring(0,1).toUpperCase(Locale.FRANCE)+s.substring(1);}
    private int dp(int value){return(int)(value*getResources().getDisplayMetrics().density+.5f);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}
    private interface DateResult{void selected(String value);}
    private abstract class SimpleWatcher implements TextWatcher{public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){}}

    private class SalesChart extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        SalesChart(){super(MainActivity.this);setPadding(dp(12),dp(12),dp(12),dp(12));}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);long[] values=store.dailySales(7);long max=1;for(long v:values)max=Math.max(max,v);float width=getWidth()-dp(30),height=getHeight()-dp(42),bar=width/7f;paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.DEFAULT);for(int i=0;i<7;i++){float left=dp(17)+i*bar+bar*.18f;float right=dp(17)+(i+1)*bar-bar*.18f;float top=dp(14)+height-(height*values[i]/max);paint.setColor(Color.rgb(209,244,239));canvas.drawRoundRect(left,dp(14),right,dp(14)+height,dp(6),dp(6),paint);paint.setColor(TURQUOISE);canvas.drawRoundRect(left,top,right,dp(14)+height,dp(6),dp(6),paint);paint.setColor(MUTED);paint.setTextSize(dp(10));canvas.drawText(dayLetter(i-6),left+(right-left)/2,getHeight()-dp(10),paint);}}
        private String dayLetter(int offset){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_MONTH,offset);return new SimpleDateFormat("EEE",Locale.FRANCE).format(c.getTime()).substring(0,1).toUpperCase(Locale.FRANCE);}
    }
}

package ar.rulosoft.mimanganu;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.fedorvlasov.lazylist.ImageLoader;

import java.util.ArrayList;
import java.util.Arrays;

import ar.rulosoft.mimanganu.adapters.ChapterAdapter;
import ar.rulosoft.mimanganu.componentes.Chapter;
import ar.rulosoft.mimanganu.componentes.ControlInfoNoScroll;
import ar.rulosoft.mimanganu.componentes.Database;
import ar.rulosoft.mimanganu.componentes.Manga;
import ar.rulosoft.mimanganu.servers.ServerBase;
import ar.rulosoft.mimanganu.services.ServicioColaDeDescarga;
import ar.rulosoft.mimanganu.utils.FragmentBusquedaAsynkTask;
import ar.rulosoft.mimanganu.utils.ThemeColors;

public class ActivityManga extends ActionBarActivity {


    public static final String DIRECCION = "direcciondelectura";
    public static final String ORDEN = "ordendecapitulos";
    public static final String CAPITULO_ID = "cap_id";
    public SwipeRefreshLayout str;
    public Manga manga;
    public Direccion direccion;
    int[] colors;
    FragmentBusquedaAsynkTask buscarNuevos;
    ListView lista;
    SharedPreferences pm;
    MenuItem sentido;
    int id;
    ControlInfoNoScroll datos;
    ImageLoader imageLoader;
    ChapterAdapter capitulosAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manga);
        id = getIntent().getExtras().getInt(ActivityMisMangas.MANGA_ID, -1);
        if (id == -1) {
            onBackPressed();
            finish();
        }
        lista = (ListView) findViewById(R.id.lista);
        str = (SwipeRefreshLayout) findViewById(R.id.str);
        imageLoader = new ImageLoader(ActivityManga.this);
        colors = ThemeColors.getColors(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()), getApplicationContext());
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(colors[0]));
        pm = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        str.setColorSchemeColors(colors[0], colors[1]);
        if (savedInstanceState == null) {
            buscarNuevos = new FragmentBusquedaAsynkTask();
            getSupportFragmentManager().beginTransaction().add(buscarNuevos, "BUSCAR_NUEVOS").commit();
        } else {
            buscarNuevos = (FragmentBusquedaAsynkTask) getSupportFragmentManager().findFragmentByTag("BUSCAR_NUEVOS");
            if (buscarNuevos.getStatus() == AsyncTask.Status.RUNNING) {
                str.post(new Runnable() {
                    @Override
                    public void run() {
                        str.setRefreshing(true);
                    }
                });
            }
        }
        str.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                buscarNuevos.iniciaTarea(manga, ActivityManga.this);
            }
        });
        lista.setDivider(new ColorDrawable(colors[0]));
        lista.setDividerHeight(1);
        datos = new ControlInfoNoScroll(ActivityManga.this);
        lista.addHeaderView(datos);
        datos.setColor(colors[0]);
        ChapterAdapter.setSELECTED(colors[1]);
        ChapterAdapter.setLigthGray(colors[0]);
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Chapter c = (Chapter) lista.getAdapter().getItem(position);
                new GetPaginas().execute(c);
            }
        });
        lista.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        lista.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                capitulosAdapter.clearSelection();
            }

            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.listitem_capitulo_menu_cab, menu);
                return true;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {

                SparseBooleanArray selection = capitulosAdapter.getSelection();
                ServerBase s = ServerBase.getServer(manga.getServerId());

                switch (item.getItemId()) {
                    case R.id.seleccionar_todo:
                        capitulosAdapter.selectAll();
                        return true;
                    case R.id.seleccionar_nada:
                        capitulosAdapter.clearSelection();
                        return true;
                    case R.id.borrar_imagenes:
                        for (int i = 0; i < selection.size(); i++) {
                            Chapter c = capitulosAdapter.getItem(selection.keyAt(i));
                            c.borrarImagenesLiberarEspacio(ActivityManga.this, manga, s);
                        }
                        break;
                    case R.id.borrar:
                        int[] selecionados = new int[selection.size()];
                        for (int j = 0; j < selection.size(); j++) {
                            selecionados[j] = selection.keyAt(j);
                        }
                        Arrays.sort(selecionados);
                        for (int i = selection.size() - 1; i >= 0; i--) {
                            Chapter c = capitulosAdapter.getItem(selection.keyAt(i));
                            c.borrar(ActivityManga.this, manga, s);
                            capitulosAdapter.remove(c);

                        }
                        break;
                    case R.id.reset:
                        for (int i = 0; i < selection.size(); i++) {
                            Chapter c = capitulosAdapter.getItem(selection.keyAt(i));
                            c.reset(ActivityManga.this, manga, s);
                        }
                        break;
                    case R.id.marcar_leido:
                        for (int i = selection.size() - 1; i >= 0; i--) {
                            Chapter c = capitulosAdapter.getItem(selection.keyAt(i));
                            c.marcarComoLeido(ActivityManga.this);
                        }
                        break;
                }
                capitulosAdapter.notifyDataSetChanged();
                mode.finish();
                return false;
            }

            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
                capitulosAdapter.setSelectedOrUnselected(position);
            }
        });
    }

    public void cargarDatos(Manga manga) {
        if (datos != null && manga != null) {
            String infoExtra = "";
            if (manga.isFinalizado()) {
                infoExtra = infoExtra + getResources().getString(R.string.finalizado);
            } else {
                infoExtra = infoExtra + getResources().getString(R.string.en_progreso);
            }
            datos.setEstado(infoExtra);
            datos.setSinopsis(manga.getSinopsis());
            datos.setServidor(ServerBase.getServer(manga.getServerId()).getServerName());
            if (manga.getAutor().length() > 1) {
                datos.setAutor(manga.getAutor());
            } else {
                datos.setAutor(getResources().getString(R.string.nodisponible));
            }
            imageLoader.DisplayImage(manga.getImages(), datos);
        }
    }

    public void cargarCapitulos(ArrayList<Chapter> chapters) {
        int fvi = 0;
        if (capitulosAdapter != null)
            fvi = lista.getFirstVisiblePosition();
        capitulosAdapter = new ChapterAdapter(this, chapters);
        if (lista != null) {
            lista.setAdapter(capitulosAdapter);
            lista.setSelection(manga.getLastIndex());
        }
        if (fvi != 0)
            lista.setSelection(fvi);
    }

    @Override
    protected void onPause() {
        int first = lista.getFirstVisiblePosition();
        Database.updateMangaLastIndex(this, manga.getId(), first);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_descargar_restantes) {
            ArrayList<Chapter> chapters = Database.getCapitulos(ActivityManga.this, this.id, Database.COL_CAP_DESCARGADO + " != 1",
                    true);
            Chapter[] arr = new Chapter[chapters.size()];
            arr = chapters.toArray(arr);
            new DascargarDemas().execute(arr);
            // TODO mecanimos mostrar progreso
            return true;
        } else if (id == R.id.action_marcar_todo_leido) {
            Database.marcarTodoComoLeido(ActivityManga.this, this.id);
            manga = Database.getFullManga(getApplicationContext(), this.id);
            cargarCapitulos(manga.getChapters());
        } else if (id == R.id.action_marcar_todo_no_leido) {
            Database.marcarTodoComoNoLeido(ActivityManga.this, this.id);
            manga = Database.getFullManga(getApplicationContext(), this.id);
            cargarCapitulos(manga.getChapters());
        } else if (id == R.id.action_sentido) {
            // TODO check database
            int direccion;
            if (manga.getSentidoLectura() != -1) {
                direccion = manga.getSentidoLectura();
            } else {
                direccion = Integer.parseInt(pm.getString(DIRECCION, "" + Direccion.R2L.ordinal()));
            }
            if (direccion == Direccion.R2L.ordinal()) {
                sentido.setIcon(R.drawable.ic_action_inverso);
                this.direccion = Direccion.L2R;
            } else if (direccion == Direccion.L2R.ordinal()) {
                sentido.setIcon(R.drawable.ic_action_verical);
                this.direccion = Direccion.VERTICAL;
            } else {
                sentido.setIcon(R.drawable.ic_action_clasico);
                this.direccion = Direccion.R2L;
            }
            manga.setSentidoLectura(this.direccion.ordinal());
            Database.updadeSentidoLectura(ActivityManga.this, this.direccion.ordinal(), manga.getId());

        } else if (id == R.id.descargas) {
            Intent intent = new Intent(this, ActivityDescargas.class);
            startActivity(intent);
        } else if (id == R.id.action_descargar_no_leidos) {
            ArrayList<Chapter> chapters = Database.getCapitulos(ActivityManga.this, ActivityManga.this.id, Database.COL_CAP_ESTADO + " < 1", true);
            Chapter[] arr = new Chapter[chapters.size()];
            arr = chapters.toArray(arr);
            new DascargarDemas().execute(arr);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        manga = Database.getFullManga(getApplicationContext(), id);
        setTitle(manga.getTitulo());
        cargarCapitulos(manga.getChapters());
        Database.updateMangaLeido(this, manga.getId());
        Database.updateMangaNuevos(ActivityManga.this, manga, -100);
        cargarDatos(manga);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_capitulos, menu);
        sentido = menu.findItem(R.id.action_sentido);
        int direccion;
        if (manga.getSentidoLectura() != -1) {
            direccion = manga.getSentidoLectura();
        } else {
            direccion = Integer.parseInt(pm.getString(DIRECCION, "" + Direccion.R2L.ordinal()));
        }

        if (direccion == Direccion.R2L.ordinal()) {
            this.direccion = Direccion.R2L;
            sentido.setIcon(R.drawable.ic_action_clasico);
        } else if (direccion == Direccion.L2R.ordinal()) {
            this.direccion = Direccion.L2R;
            sentido.setIcon(R.drawable.ic_action_inverso);
        } else {
            this.direccion = Direccion.VERTICAL;
            sentido.setIcon(R.drawable.ic_action_verical);
        }
        return true;
    }

    public enum Direccion {
        L2R, R2L, VERTICAL
    }

    public enum Orden {
        ASD, DSC
    }


    private class GetPaginas extends AsyncTask<Chapter, Void, Chapter> {
        ProgressDialog asyncdialog = new ProgressDialog(ActivityManga.this);
        String error = "";

        @Override
        protected void onPreExecute() {
            try {
                asyncdialog.setMessage(getResources().getString(R.string.iniciando));
                asyncdialog.show();
            } catch (Exception e) {
                //prevent dialog error
            }
        }

        @Override
        protected Chapter doInBackground(Chapter... arg0) {
            Chapter c = arg0[0];
            ServerBase s = ServerBase.getServer(manga.getServerId());
            try {
                if (c.getPaginas() < 1)
                    s.iniciarCapitulo(c);
            } catch (Exception e) {
                error = e.getMessage();
                e.printStackTrace();
            } finally {
                onProgressUpdate();
            }
            return c;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            asyncdialog.dismiss();
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Chapter result) {
            if (error != null && error.length() > 1) {
                Toast.makeText(ActivityManga.this, error, Toast.LENGTH_LONG).show();
            } else {
                asyncdialog.dismiss();
                Database.updateCapitulo(ActivityManga.this, result);
                ServicioColaDeDescarga.agregarDescarga(ActivityManga.this, result, true);
                int first = lista.getFirstVisiblePosition();
                Database.updateMangaLastIndex(ActivityManga.this, manga.getId(), first);
                Intent intent = new Intent(ActivityManga.this, ActivityLector.class);
                intent.putExtra(ActivityCapitulos.CAPITULO_ID, result.getId());
                ActivityManga.this.startActivity(intent);
            }
            super.onPostExecute(result);
        }
    }

    public class DascargarDemas extends AsyncTask<Chapter, Void, Void> {
        private ServerBase server;
        private Context context;

        @Override
        protected void onPreExecute() {
            server = ServerBase.getServer(ActivityManga.this.manga.getServerId());
            context = getApplicationContext();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Chapter... chapters) {
            for (Chapter c : chapters) {
                try {
                    server.iniciarCapitulo(c);
                    Database.updateCapitulo(context, c);
                    ServicioColaDeDescarga.agregarDescarga(ActivityManga.this, c, false);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return null;
        }

    }

}

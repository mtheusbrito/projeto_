package com.tecnoia.matheus.financascosmeticos.revendedor;


import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tecnoia.matheus.financascosmeticos.DAO.ConfiguracoesFirebase;
import com.tecnoia.matheus.financascosmeticos.R;
import com.tecnoia.matheus.financascosmeticos.adapters.AdapterItensVenda;
import com.tecnoia.matheus.financascosmeticos.adapters.AdapterNovaVendaDialog;
import com.tecnoia.matheus.financascosmeticos.model.ItemVenda;
import com.tecnoia.matheus.financascosmeticos.model.Produto;
import com.tecnoia.matheus.financascosmeticos.utils.GetDataFromFirebase;
import com.tecnoia.matheus.financascosmeticos.utils.ValidaCamposConexao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class NovaVendaFragment extends Fragment {
    private Toolbar toolbar;
    private TextView textViewSelecionaProduto;
    private SharedPreferences sharedPrefRevendedor;
    private String idSupervisor, idRevendedor;
    private AdapterNovaVendaDialog adapterNovaVendaDialog;
    private AlertDialog dialog;
    private Produto produto;
    private Button buttonAdicionarProdutos, buttonFinalizarVenda;
    private EditText editTextQuantidade;
    private Integer quatidadeDisponivel, quantidadeDesejada;
    private ListView listViewProdutosEmSeparação;

    private BigDecimal saldoItemAtual;
    private List<Produto> produtoList = new ArrayList<>();

    private List<ItemVenda> itemVendaList = new ArrayList<>();


    private DatabaseReference databaseVendasRealizadas;
    private ArrayList<ItemVenda> itemVendasRealizadas = new ArrayList<>();
    private BigDecimal updateSaldos;
    private BigDecimal precoProduto;


    public static NovaVendaFragment newInstance() {
        return new NovaVendaFragment();
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_nova_venda, container, false);
        setHasOptionsMenu(true);
        initViews(rootView);
        recuperaDados();
        vendasRealizadas();

        toolbarNovaVenda();

        produto = new Produto();
        popUpSelecionarPodutos();
        return rootView;
    }

    private void toolbarNovaVenda() {
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Nova Venda");

        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fm = getActivity().getSupportFragmentManager();
                fm.popBackStack();

                break;

        }


        return super.onOptionsItemSelected(item);
    }

    private void initViews(View rootView) {
        toolbar = rootView.findViewById(R.id.toolbar_nova_venda);
        buttonFinalizarVenda = rootView.findViewById(R.id.buttomFinalizarVenda);
        listViewProdutosEmSeparação = rootView.findViewById(R.id.list_view_produtos_separacao);
        textViewSelecionaProduto = rootView.findViewById(R.id.text_seleciona_produto);
        buttonAdicionarProdutos = rootView.findViewById(R.id.buttonAdicionarProdutos);
        editTextQuantidade = rootView.findViewById(R.id.edit_quantidade);
        buttonAdicionarProdutos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                verificaCampos();
            }
        });
        textViewSelecionaProduto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dialog.show();

            }
        });

        buttonFinalizarVenda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                verificaItens();
            }
        });


    }


    private void popUpSelecionarPodutos() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view1 = inflater.inflate(R.layout.adapter_dialog_lista_produtos, null);
        final ListView listViewProdutos = view1.findViewById(R.id.list_view_produtos);


        final Query referenceProdutos;
        produtoList = new ArrayList<>();


        referenceProdutos = ConfiguracoesFirebase.getListaProdutosVenda(idSupervisor, idRevendedor);
        referenceProdutos.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    produtoList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Produto produto = snapshot.getValue(Produto.class);
                        if (Integer.parseInt(produto.getQuantidade()) != 0) {
                            produtoList.add(produto);
                        }

                    }

                    adapterNovaVendaDialog.atualiza(produtoList);

                } catch (Exception e) {
                    e.printStackTrace();

                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setView(view1);
        builder.setCancelable(false);


        builder.setPositiveButton("Fechar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {


            }
        });
        dialog = builder.create();


        adapterNovaVendaDialog = new AdapterNovaVendaDialog(getActivity(), produtoList, listViewProdutos, idSupervisor, dialog, textViewSelecionaProduto, produto);
        listViewProdutos.setAdapter(adapterNovaVendaDialog);


    }


    private void recuperaDados() {
        sharedPrefRevendedor = getActivity().getPreferences(MODE_PRIVATE);
        idSupervisor = sharedPrefRevendedor.getString("idSupervisor", "");
        idRevendedor = sharedPrefRevendedor.getString("idRevendedor", "");


    }

    private void verificaCampos() {
        try {

            quatidadeDisponivel = Integer.parseInt(produto.getQuantidade());
            quantidadeDesejada = Integer.parseInt(editTextQuantidade.getText().toString());


        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean cancela = false;
        editTextQuantidade.setError(null);
        textViewSelecionaProduto.setError(null);
        if (textViewSelecionaProduto.getText().toString().equals(getString(R.string.selecione_o_produto))) {
            textViewSelecionaProduto.setError(getString(R.string.selecione_o_produto_error));
            textViewSelecionaProduto.requestFocus();
            cancela = true;

        }
        if (editTextQuantidade.getText().toString().trim().isEmpty()) {
            editTextQuantidade.setError(getString(R.string.quantidade_erro));
            editTextQuantidade.requestFocus();
            cancela = true;

        }
        if ((quantidadeDesejada != null) && (quatidadeDisponivel != null)) {
            if (quantidadeDesejada > quatidadeDisponivel) {
                editTextQuantidade.setError(getString(R.string.quantidade_indisponivel));
                editTextQuantidade.requestFocus();
                cancela = true;

            }

        } else {
            Toast.makeText(getActivity(), "Selecione um produto e indique a quantidade desejada!", Toast.LENGTH_SHORT).show();
            cancela = true;
        }

        if (!cancela) {


            adicionaProdutos();


        }


    }

    private void adicionaProdutos() {


        ItemVenda itemVenda = new ItemVenda(produto.getId(), produto.getNome(), String.valueOf(quantidadeDesejada), "0");

        itemVendaList.add(itemVenda);


        AdapterItensVenda adapterItensVenda = new AdapterItensVenda(getActivity(), itemVendaList, quantidadeDesejada);
        listViewProdutosEmSeparação.setAdapter(adapterItensVenda);
        adapterItensVenda.notifyDataSetChanged();


    }


    public void verificaItens() {

        if (itemVendaList.isEmpty()) {
            Toast.makeText(getActivity(), "0 Itens", Toast.LENGTH_SHORT).show();
        } else {
            try {


                for (int i = 0; i < itemVendaList.size(); i++) {
                    for (int j = 0; j < produtoList.size(); j++) {

                        if (itemVendaList.get(i).getId().equals(produtoList.get(j).getId())) {

                            Log.e(itemVendaList.get(i).getId(), "itemVenda");

                            ItemVenda itemVenda = itemVendaList.get(i);
                            Produto produto = produtoList.get(j);


                            Integer updateQuantidade = Integer.parseInt(produto.getQuantidade()) - Integer.parseInt(itemVenda.getQuantidade());
                            Integer updateStatus = Integer.parseInt(produto.getStatus()) + Integer.parseInt(itemVenda.getQuantidade());
                            Produto produtoUpdateEstoque = new Produto(itemVenda.getId(), itemVenda.getNome(), produto.getPreco(), String.valueOf(updateQuantidade), String.valueOf(updateStatus));
                            produtoUpdateEstoque.salvaProdutoVendas(idSupervisor, idRevendedor);

                            Integer quantidadeVendidos = Integer.parseInt(produto.getStatus()) + Integer.parseInt(itemVenda.getQuantidade());

                            for (int x = 0; x < itemVendasRealizadas.size(); x++) {

                                ItemVenda vendasRealizadas = itemVendasRealizadas.get(i);
                                if (vendasRealizadas.getId().equals(itemVenda.getId())) {
                                    saldoItemAtual = ValidaCamposConexao.formatStringToBigDecimal(vendasRealizadas.getSaldoItens());

                                }


                            }


                            if (saldoItemAtual == null) {
                                Integer unidades = Integer.parseInt(itemVenda.getQuantidade());
                                precoProduto = ValidaCamposConexao.formatStringToBigDecimal(produto.getPreco());
                                updateSaldos = precoProduto.multiply(new BigDecimal(unidades));


                                ItemVenda itemVendaVendidos = new ItemVenda(itemVenda.getId(), itemVenda.getNome(), quantidadeVendidos + "", ValidaCamposConexao.formataBigDecimalToString(updateSaldos));
                                itemVendaVendidos.vendasRealizadas(idSupervisor, idRevendedor);


                            } else {
                                Integer itemVendaQuantidade = Integer.parseInt(itemVenda.getQuantidade());
                                updateSaldos = saldoItemAtual.add(precoProduto).multiply(new BigDecimal(itemVendaQuantidade))
                                ;

                                ItemVenda itemVendaVendidos = new ItemVenda(itemVenda.getId(), itemVenda.getNome(), quantidadeVendidos + "", ValidaCamposConexao.formataBigDecimalToString(updateSaldos));
                                itemVendaVendidos.vendasRealizadas(idSupervisor, idRevendedor);


                            }


                            i++;


                        }


                    }


                }


            } catch (Exception e) {
                e.printStackTrace();
            }

            itemVendaList.clear();
            FragmentManager fm = getActivity().getSupportFragmentManager();
            fm.popBackStack();

        }

    }

    public void vendasRealizadas() {


        databaseVendasRealizadas = ConfiguracoesFirebase.getVendasRealizadas(idSupervisor, idRevendedor);
        databaseVendasRealizadas.keepSynced(true);
        new GetDataFromFirebase().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        databaseVendasRealizadas.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    itemVendasRealizadas.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        ItemVenda itemVenda = snapshot.getValue(ItemVenda.class);


                        produtoList.add(produto);

                        itemVendasRealizadas.add(itemVenda);


                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }


}




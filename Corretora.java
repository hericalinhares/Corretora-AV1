package Avaliacao1;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Corretora implements Runnable {

	private int totalOperacoes = 1000;										// total de opera��es na corretora
	private int n = 0;													    // controla o fluxo das a�oes (indices)
	private boolean corretoraOn;											// flag que avisa que corretora est� trabalhando
	
	private int numeroPermissoes = 2;										// qtde de caixas (2 caixas)
	private Semaphore caixas = new Semaphore(numeroPermissoes);				//  permite 2 acessos por vez

	private ArrayList<Ativos> meusAtivos = new ArrayList<Ativos>();			// corretora possui uma arraylist com esses ativos
	private ArrayList<Operacao> caixaGeral = new ArrayList<Operacao>();	    // corretora possui um caixa geral com todas as operacoes
	
	// -------------------------------------------------//
	// CONSTRUTOR
	public Corretora() {

		meusAtivos.add(new Ativos('A'));			// instancia de 4 novos ativos A, B, C e D
		meusAtivos.add(new Ativos('B'));
		meusAtivos.add(new Ativos('C'));
		meusAtivos.add(new Ativos('D'));


		new Thread(this).start();					// cria e inicializa a thread para esse runnable
		Thread.currentThread().setPriority(5);
	}
	
	// -------------------------------------------------//
	@Override
	public void run() {

		int time = 1;

		try {

			while(caixaGeral.size() < totalOperacoes) {		// flag para finalizar o loop do metodo run

				atualizaAtivos();
				Thread.sleep(time);

				// CORRETORA ABRE PARA CLIENTES AP�S ATIVOS TEREM 1000 DADOS HISTORICOS
				if(n == 1000) {								
					corretoraOn = true;
					System.out.println("CORRETORA ABERTA");
					time = 200;	

					// HABILITA CLIENTES A ENTRAREM NA CORRETORA
					for (int i=0; i < Cliente.getListaClientes().size(); i++) {
						new Thread(Cliente.getListaClientes().get(i)).start();
					}
				}
			}
			
			// QUANDO CORRETORA ATINGE TOTAL MAXIMO DE OPERA��ES:
			corretoraOn = false;				// FECHA CORRETORA 
			criaPlanilha();						// CRIA PLANILHA EXCEL COM DADOS

			System.out.println("THREAD CORRETORA FINALIZADA");
			
			// BLOQUEIA COMPRA DOS CLIENTES
			for (int i=0; i < Cliente.getListaClientes().size(); i++) {
				new Thread(Cliente.getListaClientes().get(i)).isInterrupted();
			}
			
			// RECONCILIA��O DE DADOS
			reconcilia��oDeDados();
			Cliente.extratoClientes();
			
			// CRIA GRAFICOS
			for(int i=0; i < meusAtivos.size(); i++) {
				new GraficoLinha(meusAtivos.get(i));
			}


		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}	
	
	// -------------------------------------------------//
	// METODO QUE ATUALIZA O ATIVO COM NOVA INFORMA��O (DISPONIVEL SOMENTE P/ OBJ CORRETORA)
	private void atualizaAtivos() {
		
		String time;
		double open, high, low, close;
		
		if(n < ImportFile.getDadosAtivos().get(0).getN()) {
			for (int i = 0; i < meusAtivos.size(); i++) {
				time 	= ImportFile.getDadosAtivos().get(i).getTimestamp().get(n);
				open 	= ImportFile.getDadosAtivos().get(i).getOpen().get(n);
				high 	= ImportFile.getDadosAtivos().get(i).getHigh().get(n);
				low 	= ImportFile.getDadosAtivos().get(i).getLow().get(n);
				close 	= ImportFile.getDadosAtivos().get(i).getClose().get(n);

				meusAtivos.get(i).addLine(time, open, high, low, close);
				meusAtivos.get(i).calcularMedias();
			}

			n++;	
		}
	}

	// METODO QUE CRIA A PLANILHA COM OS DADOS
	private void criaPlanilha() {

		ExportFile writer = new ExportFile();

		for (int i=0; i < meusAtivos.size(); i++) {
			writer.writeFile("Planilhas Exportadas\\Dados Ativo "+(i+1)+".xls",meusAtivos.get(i));		
		}
	}
		
	// METODO QUE VERIFICA A SOLICITA��O DE COMPRA DA A�AO E REGISTRA NO CAIXA GERAL DA CORRETORA
	public void solicitacaoCliente(String tipoOp, char ativo, int qtdeAtivos, int cliente) 
			throws InterruptedException {
		
		// SEMAFORO: HABILITA 2 CLIENTES A EXECUTAREM
		caixas.acquire();

		int a = 0;

		for(int i=0; i < meusAtivos.size(); i++) {
			if(meusAtivos.get(i).getId() == ativo) {
				a = i;
			}
		}

		String timestamp = meusAtivos.get(a).getTimestamp().get(n-1);
		double valorTotal = qtdeAtivos * meusAtivos.get(a).getClose().get(n-1);

		if(Cliente.getListaClientes().get(cliente-1).getCC().getSaldo() > valorTotal && tipoOp == "COMPRA") {

			if(caixaGeral.size() < totalOperacoes) {														// verifica se corretora ainda est� aberta

				// FAZ NOVA OPERA��O NO CAIXA DA CORRETORA
				caixaGeral.add(new Operacao(timestamp, tipoOp, ativo, valorTotal, cliente));

				// FAZ NOVA OPERA��O PARA CLIENTE
				Cliente.getListaClientes().get(cliente-1).getCC().novaMovimentacao
				(timestamp, tipoOp, ativo, qtdeAtivos, valorTotal);

			//	System.out.println("CLIENTE "+cliente +": "+ tipoOp +" ATIVO "+ ativo);		
			}
		} else {
		
		if(caixaGeral.size() < totalOperacoes) {	
			// FAZ NOVA OPERA��O NO CAIXA DA CORRETORA
			caixaGeral.add(new Operacao(timestamp, tipoOp, ativo, valorTotal, cliente));

			// FAZ NOVA OPERA��O PARA CLIENTE
			Cliente.getListaClientes().get(cliente-1).getCC().novaMovimentacao
			(timestamp, tipoOp, ativo, qtdeAtivos, valorTotal);

		//	System.out.println("CLIENTE "+cliente +": "+ tipoOp +" ATIVO "+ ativo);	
		}
	}
		
		caixas.release();
	}
	
	// METODO QUE RETORNA SE A CORRETORA EST� ABERTA/DISPONIVEL
	public boolean getCorretoraOn() {
		return corretoraOn;
	}
		
	// METODO QUE RETORNA MEUS ATIVOS PARA CONSULTA 
	public ArrayList<Ativos> consultaAtivos(){
		return meusAtivos;
	}
	
	// METODO QUE RETORNA INDICE DO ATIVO EM ANDAMENTO
	public int consultaAtivoAtual() {
		return (n-1);
	}
	
	public void reconcilia��oDeDados() {
				
		for(int cliente = 0; cliente < Cliente.getListaClientes().size(); cliente++) {
			
			int nCompra = 0, nVenda = 0;
			double totalCompra = 0, totalVenda = 0;
			
			for(int i = 0; i < caixaGeral.size(); i++) {
				
				if(caixaGeral.get(i).getCliente() == cliente+1) {
					
					if(caixaGeral.get(i).getTipoOperacao() == "COMPRA") {
						totalCompra += caixaGeral.get(i).getValorTotal();
						nCompra++;
						
					} else if (caixaGeral.get(i).getTipoOperacao() == "VENDA"){
						totalVenda += caixaGeral.get(i).getValorTotal();
						nVenda++;
					}
				}
			}
			
			System.out.println("CORRETORA: O cliente "+ (cliente+1) + " realizou "+nCompra+" compras de ativos no valor de "+totalCompra+ " reais. \n"
					+ "e "+nVenda+ " vendas de ativos no valor de "+totalVenda+" reais");
		}

			
	}
}

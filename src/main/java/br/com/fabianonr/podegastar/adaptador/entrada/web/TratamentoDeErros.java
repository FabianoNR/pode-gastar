package br.com.fabianonr.podegastar.adaptador.entrada.web;

import br.com.fabianonr.podegastar.aplicacao.MesNaoEncontradoException;
import br.com.fabianonr.podegastar.dominio.ItensDemaisParaCombinarException;
import br.com.fabianonr.podegastar.dominio.ValorInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class TratamentoDeErros {

  @ExceptionHandler(ValorInvalidoException.class)
  ResponseEntity<ErroResponse> valorInvalido(ValorInvalidoException excecao) {
    // Categoria ausente ou inválida já é recusada pelo Spring; aqui só chega o valor.
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErroResponse("valor", excecao.getMessage()));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException excecao) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErroResponse(excecao.getName(), "valor inválido para " + excecao.getName()));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<ErroResponse> parametroAusente(MissingServletRequestParameterException excecao) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErroResponse(
                excecao.getParameterName(), excecao.getParameterName() + " é obrigatório"));
  }

  @ExceptionHandler(ItensDemaisParaCombinarException.class)
  ResponseEntity<ErroResponse> itensDemais(ItensDemaisParaCombinarException excecao) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(new ErroResponse(null, excecao.getMessage()));
  }

  @ExceptionHandler(MesNaoEncontradoException.class)
  ResponseEntity<ErroResponse> mesNaoEncontrado(MesNaoEncontradoException excecao) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErroResponse(null, excecao.getMessage()));
  }
}

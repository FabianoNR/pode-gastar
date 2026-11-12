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
  ResponseEntity<ErroResponse> valorInvalido(ValorInvalidoException e) {
    // Categoria ausente ou inválida já é recusada pelo Spring; aqui só chega o valor.
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErroResponse("valor", e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErroResponse(e.getName(), "valor inválido para " + e.getName()));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<ErroResponse> parametroAusente(MissingServletRequestParameterException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErroResponse(e.getParameterName(), e.getParameterName() + " é obrigatório"));
  }

  @ExceptionHandler(ItensDemaisParaCombinarException.class)
  ResponseEntity<ErroResponse> itensDemais(ItensDemaisParaCombinarException e) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(new ErroResponse(null, e.getMessage()));
  }

  @ExceptionHandler(MesNaoEncontradoException.class)
  ResponseEntity<ErroResponse> mesNaoEncontrado(MesNaoEncontradoException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse(null, e.getMessage()));
  }
}

package uteq.edu.ec.sicrec.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.sicrec.entity.Favorito;
import uteq.edu.ec.sicrec.repository.FavoritoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;

    public FavoritoService(FavoritoRepository favoritoRepository) {
        this.favoritoRepository = favoritoRepository;
    }

    public Favorito guardar(Favorito favorito) {

        if (favoritoRepository
                .findByUsuarioIdAndSourceId(
                        favorito.getUsuarioId(),
                        favorito.getSourceId()
                )
                .isPresent()) {

            throw new RuntimeException("La revista ya está en favoritos.");

        }

        favorito.setFechaGuardado(LocalDateTime.now());

        return favoritoRepository.save(favorito);

    }

    public List<Favorito> listar(Long usuarioId) {

        return favoritoRepository.findByUsuarioId(usuarioId);

    }

    public void eliminar(Long id) {

        favoritoRepository.deleteById(id);

    }

}
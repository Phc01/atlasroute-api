package com.atlasroute.api.user;

import com.atlasroute.api.error.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Usuário dono do token. 404 se ele foi removido depois de o token ser emitido. */
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
    }

    /**
     * Lista paginada, sempre ordenada por id. A ordenação vinda da requisição é ignorada de
     * propósito: aceitar qualquer campo permitiria ordenar por {@code passwordHash} e daria 500
     * para nomes de campo inexistentes.
     */
    public Page<UserResponse> list(Pageable pageable) {
        Pageable byId = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
        return userRepository.findAll(byId).map(UserResponse::from);
    }
}

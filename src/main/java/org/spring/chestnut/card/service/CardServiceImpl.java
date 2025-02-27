package org.spring.chestnut.card.service;

import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.spring.chestnut.card.dto.CardMoveRequest;
import org.spring.chestnut.card.dto.CardRequest;
import org.spring.chestnut.card.dto.CardResponse;
import org.spring.chestnut.card.dto.WorKerRequest;
import org.spring.chestnut.card.entity.CardEntity;
import org.spring.chestnut.card.entity.WorkerEntity;
import org.spring.chestnut.card.repository.CardRepository;
import org.spring.chestnut.card.repository.WorkerRepository;
import org.spring.chestnut.global.execption.custom.NotFoundException;
import org.spring.chestnut.global.execption.custom.WorkerException;
import org.spring.chestnut.global.aop.Lockable;
import org.spring.chestnut.global.security.UserDetailsImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
@Service
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final WorkerRepository workerRepository;

    @Override
    public CardResponse createCard(Long boardId, Long columnId, CardRequest request,
        UserDetails member) {

        CardEntity cardEntity = CardEntity.of(columnId, request);

        CardEntity save = cardRepository.save(cardEntity);

        return new CardResponse(save);
    }

    @Override
    public CardResponse updateCard(Long cardId, CardRequest request,
        UserDetails member) {

        CardEntity cardEntity = cardRepository.findById(cardId)
            .orElseThrow(() -> new NotFoundException("없는 카드입니다."));

        cardEntity.updateCard(request);
        CardEntity updateCard = cardRepository.save(cardEntity);

        List<Long> workerList = workerRepository.findByCardId(cardId).stream()
            .map(WorkerEntity::getMemberId)
            .toList();

        return new CardResponse(updateCard, workerList);
    }

    @Override
    public void deleteCard(Long cardId, UserDetails member) {

        CardEntity cardEntity = cardRepository.findById(cardId)
            .orElseThrow(() -> new NotFoundException("없는 카드입니다."));

        cardRepository.delete(cardEntity);
        workerRepository.deleteByCardId(cardId);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getCardByCardId(Long cardId) {

        CardEntity cardEntity = cardRepository.findById(cardId)
            .orElseThrow(() -> new NotFoundException("없는 카드입니다."));

        List<Long> workers = workerRepository.findByCardId(cardId).stream()
            .map(WorkerEntity::getMemberId)
            .toList();

        return new CardResponse(cardEntity, workers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> getCardsByColumnId(Long columnId) {
        return cardRepository.findAllCardsByColumnId(columnId);
    }

    @Override
    public CardResponse updateWorkers(Long cardId, WorKerRequest worKerRequest,
        UserDetailsImpl userDetails) {

        CardEntity card = cardRepository.findById(cardId)
            .orElseThrow(() -> new NotFoundException("없는 카드입니다."));

        List<WorkerEntity> workerLists = workerRepository.findByCardIdOrderByMemberId(cardId);

        List<Long> collect = workerLists.stream()
            .map(WorkerEntity::getMemberId)
            .toList();

        if (worKerRequest.getAddRequest() != null) {
            List<Long> addRequest = worKerRequest.getAddRequest().getWorkerList();
            if (!addRequest.isEmpty() && new HashSet<>(collect).containsAll(addRequest)) {
                throw new WorkerException("이미 있는 작업자입니다.");
            } else {
                addRequest.forEach(
                    t -> workerRepository.save(WorkerEntity.of(cardId, t))
                );
            }
        }

        if (worKerRequest.getRemoveRequest() != null) {
            List<Long> removeRequest = worKerRequest.getRemoveRequest().getWorkerList();
            if (!new HashSet<>(collect).containsAll(removeRequest)) {
                throw new WorkerException("없는 작업자입니다.");
            } else {
                workerLists.stream().filter(
                    t -> removeRequest.contains(t.getMemberId())
                ).forEach(workerRepository::delete);
            }
        }

        List<Long> updateWorkers = workerRepository.findByCardId(cardId).stream()
            .map(WorkerEntity::getMemberId)
            .toList();

        return new CardResponse(card, updateWorkers);
    }

    @Override
    @Lockable(value = "moveCard", waitTime = 5000, leaseTime = 100)
    public CardResponse moveCard(Long cardId, CardMoveRequest request,
        UserDetailsImpl userDetails) {

        CardEntity card = cardRepository.findById(cardId)
            .orElseThrow(() -> new NotFoundException("없는 카드입니다."));

        card.moveCard(request.getMoveTo());
        CardEntity save = cardRepository.save(card);

        List<Long> workers = workerRepository.findByCardId(cardId)
            .stream().map(WorkerEntity::getMemberId)
            .toList();

        return new CardResponse(save, workers);
    }
}

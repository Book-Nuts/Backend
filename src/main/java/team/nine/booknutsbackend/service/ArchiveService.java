package team.nine.booknutsbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.nine.booknutsbackend.domain.Board;
import team.nine.booknutsbackend.domain.User;
import team.nine.booknutsbackend.domain.archive.Archive;
import team.nine.booknutsbackend.domain.archive.ArchiveBoard;
import team.nine.booknutsbackend.dto.response.ArchiveResponse;
import team.nine.booknutsbackend.dto.response.BoardResponse;
import team.nine.booknutsbackend.exception.archive.ArchiveDuplicateException;
import team.nine.booknutsbackend.exception.archive.ArchiveNotFoundException;
import team.nine.booknutsbackend.exception.board.BoardNotFoundException;
import team.nine.booknutsbackend.exception.board.NoAccessException;
import team.nine.booknutsbackend.repository.ArchiveBoardRepository;
import team.nine.booknutsbackend.repository.ArchiveRepository;
import team.nine.booknutsbackend.repository.BoardRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ArchiveService {

    private final ArchiveRepository archiveRepository;
    private final ArchiveBoardRepository archiveBoardRepository;
    private final BoardRepository boardRepository;

    //아카이브 리스트 조회
    @Transactional(readOnly = true)
    public List<ArchiveResponse> getArchiveList(User user) {
        List<Archive> archives = archiveRepository.findAllByOwner(user);
        List<ArchiveResponse> archiveResponseList = new ArrayList<>();

        for (Archive archive : archives) {
            archiveResponseList.add(ArchiveResponse.archiveResponse(archive));
        }

        Collections.reverse(archiveResponseList); //최신순
        return archiveResponseList;
    }

    //아카이브 생성
    @Transactional
    public Archive createArchive(Archive archive) {
        return archiveRepository.save(archive);
    }

    //특정 아카이브 조회
    @Transactional(readOnly = true)
    public List<BoardResponse> findArchive(Long archiveId, User user) throws ArchiveNotFoundException {
        Archive archive = archiveRepository.findById(archiveId)
                .orElseThrow(() -> new ArchiveNotFoundException("존재하지 않는 아카이브 아이디입니다."));
        List<ArchiveBoard> archiveBoards = archiveBoardRepository.findByArchive(archive);
        List<BoardResponse> boardList = new ArrayList<>();

        for (ArchiveBoard archiveBoard : archiveBoards) {
            boardList.add(BoardResponse.boardResponse(archiveBoard.getBoard(), user));
        }

        Collections.reverse(boardList); //최신순
        return boardList;
    }

    //아카이브에 게시글 추가
    @Transactional
    public void addPostToArchive(Long archiveId, Long boardId, User user) {
        Archive archive = archiveRepository.findById(archiveId)
                .orElseThrow(() -> new ArchiveNotFoundException("존재하지 않는 아카이브 아이디입니다."));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("존재하지 않는 게시글 번호입니다."));

        //아카이브 중복체크
        if (archiveBoardRepository.findByBoardAndOwner(board, user).isPresent())
            throw new ArchiveDuplicateException("이미 아카이브에 게시글이 존재합니다");

        ArchiveBoard archiveBoard = new ArchiveBoard();
        archiveBoard.setArchive(archive);
        archiveBoard.setBoard(board);
        archiveBoard.setOwner(archive.getOwner());
        archiveBoardRepository.save(archiveBoard);
    }

    //아카이브 삭제
    @Transactional
    public void deleteArchive(Long archiveId, User user) throws NoAccessException {
        Archive archive = archiveRepository.findByArchiveIdAndOwner(archiveId, user)
                .orElseThrow(() -> new NoAccessException("해당 유저는 삭제 권한이 없습니다."));
        List<ArchiveBoard> archiveBoards = archiveBoardRepository.findByArchive(archive);

        archiveBoardRepository.deleteAll(archiveBoards);
        archiveRepository.delete(archive);
    }

    //아카이브 내의 게시글 삭제
    @Transactional
    public void deleteArchivePost(Long archiveId, Long boardId) {
        Archive archive = archiveRepository.findById(archiveId)
                .orElseThrow(() -> new ArchiveNotFoundException("존재하지 않는 아카이브 아이디입니다."));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("존재하지 않는 게시글 번호입니다."));

        ArchiveBoard archiveBoard = archiveBoardRepository.findByArchiveAndBoard(archive, board);
        archiveBoardRepository.delete(archiveBoard);
    }

    //아카이브 조회 (아카이브명, 내용, 이미지)
    @Transactional(readOnly = true)
    public Archive findByArchiveId(Long archiveId) throws ArchiveNotFoundException {
        return archiveRepository.findById(archiveId)
                .orElseThrow(() -> new BoardNotFoundException("존재하지 않는 아카이브 아이디입니다."));
    }

    //아카이브 수정
    @Transactional
    public Archive updateArchive(Archive archive, User user) throws NoAccessException {
        archiveRepository.findByArchiveIdAndOwner(archive.getArchiveId(), user)
                .orElseThrow(() -> new NoAccessException("해당 유저는 수정 권한이 없습니다."));

        return archiveRepository.save(archive);
    }

}
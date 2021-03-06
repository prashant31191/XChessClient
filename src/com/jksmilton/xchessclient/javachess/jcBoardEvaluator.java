/*************************************************************************
 * jcBoardEvaluator - Analyzes and evaluates a chess board position
 * by F.D. Laramee
 *
 * History
 * 07.08.00 Creation
 * *********************************************************************/

package com.jksmilton.xchessclient.javachess;

import com.jksmilton.xchessclient.javachess.jcBoard;

public class jcBoardEvaluator
{

  /**********************************************************************
   * DATA MEMBERS
   **********************************************************************/

  // Data counters to evaluate pawn structure
  int MaxPawnFileBins[];
  int MaxPawnColorBins[];
  int MaxTotalPawns;
  int PawnRams;
  int MaxMostAdvanced[];
  int MaxPassedPawns[];
  int MinPawnFileBins[];
  int MinMostBackward[];

  // The "graininess" of the evaluation.  MTD(f) works a lot faster if the
  // evaluation is relatively coarse
  private static final int Grain = 3;

  /**********************************************************************
   * PUBLIC METHODS
   *********************************************************************/

  // Construction
  public jcBoardEvaluator()
  {
    MaxPawnFileBins = new int[ 8 ];
    MaxPawnColorBins = new int[ 2 ];
    MaxMostAdvanced = new int[ 8 ];
    MaxPassedPawns = new int[ 8 ];
    MinPawnFileBins = new int[ 8 ];
    MinMostBackward = new int[ 8 ];
  }

  // int EvaluateQuickie( jcBoard theBoard, int FromWhosePerspective )
  // A simple, fast evaluation based exclusively on material.  Since material
  // is overwhelmingly more important than anything else, we assume that if a
  // position's material value is much lower (or much higher) than another,
  // then there is no need to waste time on positional factors because they
  // won't be enough to tip the scales the other way, so to speak.
  public int EvaluateQuickie( jcBoard theBoard, int fromWhosePerspective )
  {
    return ( ( theBoard.EvalMaterial( fromWhosePerspective ) >> Grain ) << Grain );
  }

  // int EvaluateComplete( jcBoard theBoard )
  // A detailed evaluation function, taking into account several positional
  // factors
  public int EvaluateComplete( jcBoard theBoard, int fromWhosePerspective )
  {
    AnalyzePawnStructure( theBoard, fromWhosePerspective );
    return(((theBoard.EvalMaterial( fromWhosePerspective ) +
             EvalPawnStructure( fromWhosePerspective ) +
             EvalBadBishops( theBoard, fromWhosePerspective ) +
             EvalDevelopment( theBoard, fromWhosePerspective ) +
             EvalRookBonus( theBoard, fromWhosePerspective ) +
             EvalKingTropism( theBoard, fromWhosePerspective ) ) >> Grain ) << Grain );
  }

  /***************************************************************************
   * PRIVATE METHODS
   **************************************************************************/

  // private EvalKingTropism
  // All other things being equal, having your Knights, Queens and Rooks close
  // to the opponent's king is a good thing
  // This method is a bit slow and dirty, but it gets the job done
  private int EvalKingTropism( jcBoard theBoard, int fromWhosePerspective )
  {
    int score = 0;

    // Square coordinates
    int kingRank = 0, kingFile = 0;
    int pieceRank = 0, pieceFile = 0;

    if ( fromWhosePerspective == jcPlayer.SIDE_WHITE )
    {
      // Look for enemy king first!
      for( int i = 0; i < 64; i++ )
      {
        if ( theBoard.FindBlackPiece( i ) == jcBoard.BLACK_KING )
        {
          kingRank = i >> 8;
          kingFile = i % 8;
          break;
        }
      }

      // Now, look for pieces which need to be evaluated
      for( int i = 0; i < 64; i++ )
      {
        pieceRank = i >> 8;
        pieceFile = i % 8;
        switch( theBoard.FindWhitePiece( i ) )
        {
          case jcBoard.WHITE_ROOK:
            score -= ( Math.min( Math.abs( kingRank - pieceRank ),
                                 Math.abs( kingFile - pieceFile ) ) << 1 );
            break;
          case jcBoard.WHITE_KNIGHT:
            score += 5 - Math.abs( kingRank - pieceRank ) -
                         Math.abs( kingFile - pieceFile );
            break;
          case jcBoard.WHITE_QUEEN:
            score -= Math.min( Math.abs( kingRank - pieceRank ),
                               Math.abs( kingFile - pieceFile ) );
            break;
          default:
            break;
        }
      }
    }
    else
    {
      // Look for enemy king first!
      for( int i = 0; i < 64; i++ )
      {
        if ( theBoard.FindWhitePiece( i ) == jcBoard.WHITE_KING )
        {
          kingRank = i >> 8;
          kingFile = i % 8;
          break;
        }
      }

      // Now, look for pieces which need to be evaluated
      for( int i = 0; i < 64; i++ )
      {
        pieceRank = i >> 8;
        pieceFile = i % 8;
        switch( theBoard.FindBlackPiece( i ) )
        {
          case jcBoard.BLACK_ROOK:
            score -= ( Math.min( Math.abs( kingRank - pieceRank ),
                                 Math.abs( kingFile - pieceFile ) ) << 1 );
            break;
          case jcBoard.BLACK_KNIGHT:
            score += 5 - Math.abs( kingRank - pieceRank ) -
                         Math.abs( kingFile - pieceFile );
            break;
          case jcBoard.BLACK_QUEEN:
            score -= Math.min( Math.abs( kingRank - pieceRank ),
                               Math.abs( kingFile - pieceFile ) );
            break;
          default:
            break;
        }
      }
    }
    return score;
  }

  // private EvalRookBonus
  // Rooks are more effective on the seventh rank, on open files and behind
  // passed pawns
  private int EvalRookBonus( jcBoard theBoard, int fromWhosePerspective )
  {
    long rookboard = theBoard.GetBitBoard( jcBoard.ROOK + fromWhosePerspective );
    if ( rookboard == 0 )
      return 0;

    int score = 0;
    for( int square = 0; square < 64; square++ )
    {
      // Find a rook
      if ( ( rookboard & jcBoard.SquareBits[ square ] ) != 0 )
      {
        // Is this rook on the seventh rank?
        int rank = ( square >> 3 );
        int file = ( square % 8 );
        if ( ( fromWhosePerspective == jcPlayer.SIDE_WHITE ) &&
             ( rank == 1 ) )
          score += 22;
        if ( ( fromWhosePerspective == jcPlayer.SIDE_BLACK ) &&
             ( rank == 7 ) )
          score += 22;

        // Is this rook on a semi- or completely open file?
        if ( MaxPawnFileBins[ file ] == 0 )
        {
          if ( MinPawnFileBins[ file ] == 0 )
            score += 10;
          else
            score += 4;
        }

        // Is this rook behind a passed pawn?
        if ( ( fromWhosePerspective == jcPlayer.SIDE_WHITE ) &&
             ( MaxPassedPawns[ file ] < square ) )
            score += 25;
        if ( ( fromWhosePerspective == jcPlayer.SIDE_BLACK ) &&
             ( MaxPassedPawns[ file ] > square ) )
            score += 25;

        // Use the bitboard erasure trick to avoid looking for additional
        // rooks once they have all been seen
        rookboard ^= jcBoard.SquareBits[ square ];
        if ( rookboard == 0 )
          break;
      }
    }
    return score;
  }

  // private EvalDevelopment
  // Mostly useful in the opening, this term encourages the machine to move
  // its bishops and knights into play, to control the center with its queen's
  // and king's pawns, and to castle if the opponent has many major pieces on
  // the board
  private int EvalDevelopment( jcBoard theBoard, int fromWhosePerspective )
  {
    int score = 0;

    if ( fromWhosePerspective == jcPlayer.SIDE_WHITE )
    {
      // Has the machine advanced its center pawns?
      if ( theBoard.FindWhitePiece( 51 ) == jcBoard.WHITE_PAWN )
        score -= 15;
      if ( theBoard.FindWhitePiece( 52 ) == jcBoard.WHITE_PAWN )
        score -= 15;

      // Penalize bishops and knights on the back rank
      for( int square = 56; square < 64; square++ )
      {
        if ( ( theBoard.FindWhitePiece( square ) == jcBoard.WHITE_KNIGHT ) ||
             ( theBoard.FindWhitePiece( square ) == jcBoard.WHITE_BISHOP ) )
          score -= 10;
      }

      // Penalize too-early queen movement
      long queenboard = theBoard.GetBitBoard( jcBoard.WHITE_QUEEN );
      if ( ( queenboard != 0 ) && ( ( queenboard & jcBoard.SquareBits[ 59 ] ) == 0 ) )
      {
        // First, count friendly pieces on their original squares
        int cnt = 0;
        if ( ( theBoard.GetBitBoard( jcBoard.WHITE_BISHOP ) & jcBoard.SquareBits[ 58 ] ) != 0 )
          cnt++;
        if ( ( theBoard.GetBitBoard( jcBoard.WHITE_BISHOP ) & jcBoard.SquareBits[ 61 ] ) != 0 )
          cnt++;
        if ( ( theBoard.GetBitBoard( jcBoard.WHITE_KNIGHT ) & jcBoard.SquareBits[ 57 ] ) != 0 )
          cnt++;
        if ( ( theBoard.GetBitBoard( jcBoard.WHITE_KNIGHT ) & jcBoard.SquareBits[ 62 ] ) != 0 )
          cnt++;
        if ( ( theBoard.GetBitBoard( jcBoard.WHITE_ROOK ) & jcBoard.SquareBits[ 56 ] ) != 0 )
          cnt++;
        if ( ( theBoard.GetBitBoard( jcBoard.WHITE_ROOK ) & jcBoard.SquareBits[ 63 ] ) != 0 )
          cnt++;
        if ( ( theBoard.GetBitBoard( jcBoard.WHITE_KING ) & jcBoard.SquareBits[ 60 ] ) != 0 )
          cnt++;
        score -= ( cnt << 3 );
      }

      // And finally, incite castling when the enemy has a queen on the board
      // This is a slightly simpler version of a factor used by Cray Blitz
      if ( theBoard.GetBitBoard( jcBoard.BLACK_QUEEN ) != 0 )
      {
        // Being castled deserves a bonus
        if ( theBoard.GetHasCastled( jcPlayer.SIDE_WHITE ) )
          score += 10;
        // small penalty if you can still castle on both sides
        else if ( theBoard.GetCastlingStatus( jcPlayer.SIDE_WHITE + jcBoard.CASTLE_QUEENSIDE ) &&
                  theBoard.GetCastlingStatus( jcPlayer.SIDE_WHITE + jcBoard.CASTLE_QUEENSIDE ) )
          score -= 24;
        // bigger penalty if you can only castle kingside
        else if ( theBoard.GetCastlingStatus( jcPlayer.SIDE_WHITE + jcBoard.CASTLE_KINGSIDE ) )
          score -= 40;
        // bigger penalty if you can only castle queenside
        else if ( theBoard.GetCastlingStatus( jcPlayer.SIDE_WHITE + jcBoard.CASTLE_QUEENSIDE ) )
          score -= 80;
        // biggest penalty if you can't castle at all
        else
          score -= 120;
      }
    }
    else // from black's perspective
    {
      // Has the machine advanced its center pawns?
      if ( theBoard.FindBlackPiece( 11 ) == jcBoard.BLACK_PAWN )
        score -= 15;
      if ( theBoard.FindBlackPiece( 12 ) == jcBoard.BLACK_PAWN )
        score -= 15;

      // Penalize bishops and knights on the back rank
      for( int square = 0; square < 8; square++ )
      {
        if ( ( theBoard.FindBlackPiece( square ) == jcBoard.BLACK_KNIGHT ) ||
             ( theBoard.FindBlackPiece( square ) == jcBoard.BLACK_BISHOP ) )
          score -= 10;
      }

      // Penalize too-early queen movement
      long queenboard = theBoard.GetBitBoard( jcBoard.BLACK_QUEEN );
      if ( ( queenboard != 0 ) && ( ( queenboard & jcBoard.SquareBits[ 3 ] ) == 0 ) )
      {
        // First, count friendly pieces on their original squares
        int cnt = 0;
        if ( ( theBoard.GetBitBoard( jcBoard.BLACK_BISHOP ) & jcBoard.SquareBits[ 2 ] ) != 0 )
          cnt++;
        if ( ( theBoard.GetBitBoard( jcBoard.BLACK_BISHOP ) & jcBoard.SquareBits[ 5 ] ) != 0 )
          cnt++;
        if ( ( theBoard.GetBitBoard( jcBoard.BLACK_KNIGHT ) & jcBoard.SquareBits[ 1 ] ) != 0 )
          cnt++;
        if ( ( theBoard.GetBitBoard( jcBoard.BLACK_KNIGHT ) & jcBoard.SquareBits[ 6 ] ) != 0 )
          cnt++;
        if ( ( theBoard.GetBitBoard( jcBoard.BLACK_ROOK ) & jcBoard.SquareBits[ 0 ] ) != 0 )
          cnt++;
        if ( ( theBoard.GetBitBoard( jcBoard.BLACK_ROOK ) & jcBoard.SquareBits[ 7 ] ) != 0 )
          cnt++;
        if ( ( theBoard.GetBitBoard( jcBoard.BLACK_KING ) & jcBoard.SquareBits[ 4 ] ) != 0 )
          cnt++;
        score -= ( cnt << 3 );
      }

      // And finally, incite castling when the enemy has a queen on the board
      // This is a slightly simpler version of a factor used by Cray Blitz
      if ( theBoard.GetBitBoard( jcBoard.WHITE_QUEEN ) != 0 )
      {
        // Being castled deserves a bonus
        if ( theBoard.GetHasCastled( jcPlayer.SIDE_BLACK ) )
          score += 10;
        // small penalty if you can still castle on both sides
        else if ( theBoard.GetCastlingStatus( jcPlayer.SIDE_BLACK + jcBoard.CASTLE_QUEENSIDE ) &&
                  theBoard.GetCastlingStatus( jcPlayer.SIDE_BLACK + jcBoard.CASTLE_QUEENSIDE ) )
          score -= 24;
        // bigger penalty if you can only castle kingside
        else if ( theBoard.GetCastlingStatus( jcPlayer.SIDE_BLACK + jcBoard.CASTLE_KINGSIDE ) )
          score -= 40;
        // bigger penalty if you can only castle queenside
        else if ( theBoard.GetCastlingStatus( jcPlayer.SIDE_BLACK + jcBoard.CASTLE_QUEENSIDE ) )
          score -= 80;
        // biggest penalty if you can't castle at all
        else
          score -= 120;
      }
    }
    return score;
  }

  // private EvalBadBishops
  // If Max has too many pawns on squares of the color of his surviving bishops,
  // the bishops may be limited in their movement
  private int EvalBadBishops( jcBoard theBoard, int fromWhosePerspective )
  {
    long where = theBoard.GetBitBoard( jcBoard.BISHOP + fromWhosePerspective );
    if ( where == 0 )
      return 0;

    int score = 0;
    for( int square = 0; square < 64; square++ )
    {
      // Find a bishop
      if ( ( where & jcBoard.SquareBits[ square ] ) != 0 )
      {
        // What is the bishop's square color?
        int rank = ( square >> 3 );
        int file = ( square % 8 );
        if ( ( rank % 2 ) == ( file % 2 ) )
          score -= ( MaxPawnColorBins[ 0 ] << 3 );
        else
          score -= ( MaxPawnColorBins[ 1 ] << 3 );

        // Use the bitboard erasure trick to avoid looking for additional
        // bishops once they have all been seen
        where ^= jcBoard.SquareBits[ square ];
        if ( where == 0 )
          break;
      }
    }
    return score;
  }

  // private EvalPawnStructure
  // Given the pawn formations, penalize or bonify the position according to
  // the features it contains
  private int EvalPawnStructure( int fromWhosePerspective )
  {
    int score = 0;

    // First, look for doubled pawns
    // In chess, two or more pawns on the same file usually hinder each other,
    // so we assign a minor penalty
    for( int bin = 0; bin < 8; bin++ )
      if ( MaxPawnFileBins[ bin ] > 1 )
        score -= 8;

    // Now, look for an isolated pawn, i.e., one which has no neighbor pawns
    // capable of protecting it from attack at some point in the future
    if ( ( MaxPawnFileBins[ 0 ] > 0 ) && ( MaxPawnFileBins[ 1 ] == 0 ) )
      score -= 15;
    if ( ( MaxPawnFileBins[ 7 ] > 0 ) && ( MaxPawnFileBins[ 6 ] == 0 ) )
      score -= 15;
    for( int bin = 1; bin < 7; bin++ )
    {
      if ( ( MaxPawnFileBins[ bin ] > 0 ) && ( MaxPawnFileBins[ bin - 1 ] == 0 )
           && ( MaxPawnFileBins[ bin + 1 ] == 0 ) )
        score -= 15;
    }

    // Assign a small penalty to positions in which Max still has all of his
    // pawns; this incites a single pawn trade (to open a file), but not by
    // much
    if ( MaxTotalPawns == 8 )
      score -= 10;

    // Penalize pawn rams, because they restrict movement
    score -= 8 * PawnRams;

    // Finally, look for a passed pawn; i.e., a pawn which can no longer be
    // blocked or attacked by a rival pawn
    if ( fromWhosePerspective == jcPlayer.SIDE_WHITE )
    {
      if ( MaxMostAdvanced[ 0 ] < Math.min( MinMostBackward[ 0 ], MinMostBackward[ 1 ] ) )
        score += ( 8 - ( MaxMostAdvanced[ 0 ] >> 3 ) ) *
                 ( 8 - ( MaxMostAdvanced[ 0 ] >> 3 ) );
      if ( MaxMostAdvanced[ 7 ] < Math.min( MinMostBackward[ 7 ], MinMostBackward[ 6 ] ) )
        score += ( 8 - ( MaxMostAdvanced[ 7 ] >> 3 ) ) *
                 ( 8 - ( MaxMostAdvanced[ 7 ] >> 3 ) );
      for( int i = 1; i < 7; i++ )
      {
        if ( ( MaxMostAdvanced[ i ] < MinMostBackward[ i ] ) &&
             ( MaxMostAdvanced[ i ] < MinMostBackward[ i - 1 ] ) &&
             ( MaxMostAdvanced[ i ] < MinMostBackward[ i + 1 ] ) )
          score += ( 8 - ( MaxMostAdvanced[ i ] >> 3 ) ) *
                   ( 8 - ( MaxMostAdvanced[ i ] >> 3 ) );
      }
    }
    else // from Black's perspective
    {
      if ( MaxMostAdvanced[ 0 ] > Math.max( MinMostBackward[ 0 ], MinMostBackward[ 1 ] ) )
        score += ( MaxMostAdvanced[ 0 ] >> 3 ) *
                 ( MaxMostAdvanced[ 0 ] >> 3 );
      if ( MaxMostAdvanced[ 7 ] > Math.max( MinMostBackward[ 7 ], MinMostBackward[ 6 ] ) )
        score += ( MaxMostAdvanced[ 7 ] >> 3 ) *
                 ( MaxMostAdvanced[ 7 ] >> 3 );
      for( int i = 1; i < 7; i++ )
      {
        if ( ( MaxMostAdvanced[ i ] > MinMostBackward[ i ] ) &&
             ( MaxMostAdvanced[ i ] > MinMostBackward[ i - 1 ] ) &&
             ( MaxMostAdvanced[ i ] > MinMostBackward[ i + 1 ] ) )
          score += ( MaxMostAdvanced[ i ] >> 3 ) *
                   ( MaxMostAdvanced[ i ] >> 3 );
      }
    }

    return score;
  }

  // private AnalyzePawnStructure
  // Look at pawn positions to be able to detect features such as doubled,
  // isolated or passed pawns
  private boolean AnalyzePawnStructure( jcBoard theBoard, int fromWhosePerspective )
  {
    // Reset the counters
    for( int i = 0; i < 8; i++ )
    {
      MaxPawnFileBins[ i ] = 0;
      MinPawnFileBins[ i ] = 0;
    }
    MaxPawnColorBins[ 0 ] = 0;
    MaxPawnColorBins[ 1 ] = 0;
    PawnRams = 0;
    MaxTotalPawns = 0;

    // Now, perform the analysis
    if ( fromWhosePerspective == jcPlayer.SIDE_WHITE )
    {
      for( int i = 0; i < 8; i++ )
      {
        MaxMostAdvanced[ i ] = 63;
        MinMostBackward[ i ] = 63;
        MaxPassedPawns[ i ] = 63;
      }
      for( int square = 55; square >= 8; square-- )
      {
        // Look for a white pawn first, and count its properties
        if ( theBoard.FindWhitePiece( square ) == jcBoard.WHITE_PAWN )
        {
          // What is the pawn's position, in rank-file terms?
          int rank = square >> 3;
          int file = square % 8;

          // This pawn is now the most advanced of all white pawns on its file
          MaxPawnFileBins[ file ]++;
          MaxTotalPawns++;
          MaxMostAdvanced[ file ] = square;

          // Is this pawn on a white or a black square?
          if ( ( rank % 2 ) == ( file % 2 ) )
            MaxPawnColorBins[ 0 ]++;
          else
            MaxPawnColorBins[ 1 ]++;

          // Look for a "pawn ram", i.e., a situation where a black pawn
          // is located in the square immediately ahead of this one.
          if ( theBoard.FindBlackPiece( square - 8 ) == jcBoard.BLACK_PAWN )
            PawnRams++;
        }
        // Now, look for a BLACK pawn
        else if ( theBoard.FindBlackPiece( square ) == jcBoard.BLACK_PAWN )
        {
          // If the black pawn exists, it is the most backward found so far
          // on its file
          int file = square % 8;
          MinPawnFileBins[ file ]++;
          MinMostBackward[ file ] = square;
        }
      }
    }
    else // Analyze from Black's perspective
    {
      for( int i = 0; i < 8; i++ )
      {
        MaxMostAdvanced[ i ] = 0;
        MaxPassedPawns[ i ] = 0;
        MinMostBackward[ i ] = 0;
      }
      for( int square = 8; square < 56; square++ )
      {
        if ( theBoard.FindBlackPiece( square ) == jcBoard.BLACK_PAWN )
        {
          // What is the pawn's position, in rank-file terms?
          int rank = square >> 3;
          int file = square % 8;

          // This pawn is now the most advanced of all white pawns on its file
          MaxPawnFileBins[ file ]++;
          MaxTotalPawns++;
          MaxMostAdvanced[ file ] = square;

          if ( ( rank % 2 ) == ( file % 2 ) )
            MaxPawnColorBins[ 0 ]++;
          else
            MaxPawnColorBins[ 1 ]++;

          if ( theBoard.FindWhitePiece( square + 8 ) == jcBoard.WHITE_PAWN )
            PawnRams++;
        }
        else if ( theBoard.FindWhitePiece( square ) == jcBoard.WHITE_PAWN )
        {
          int file = square % 8;
          MinPawnFileBins[ file ]++;
          MinMostBackward[ file ] = square;
        }
      }
    }
    return true;
  }
}
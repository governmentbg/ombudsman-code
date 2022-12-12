<?php

namespace App\Http\Controllers;

use Illuminate\Support\Facades\App;
use Illuminate\Support\Facades\DB;
use Illuminate\Http\Request;

class MPositionController extends Controller
{
    public static function pstList($count, $type = 1)
    {



        $PstList = DB::table('m_position as n')
            ->whereNull('n.deleted_at')
            ->where('n.Pst_doc_type', $type)
            ->select(
                'n.*',
                DB::raw("IF(SUBSTR(Pst_file, 1,4)='pub/',CONCAT('/',Pst_file),CONCAT('/pub/files/',Pst_file))  as Pst_file"),
            )
            ->orderBy('n.Pst_date', 'desc')
            ->orderBy('n.Pst_id', 'desc')
            ->take($count)
            ->get();





        return $PstList;
    }

    public static function getPos($locale, $id)
    {

        // dd($id);

        $data = DB::table('m_position as n')
            ->whereNull('n.deleted_at')
            ->where('n.Pst_path', $id)
            ->select(
                'n.*',
                DB::raw("IF(SUBSTR(Pst_file, 1,4)='pub/',CONCAT('/',Pst_file),CONCAT('/pub/files/',Pst_file))  as Pst_file"),
            )
            ->orderBy('n.Pst_date', 'desc')
            ->orderBy('n.Pst_id', 'desc')
            ->first();

        if (!$data) {
            // pass to 404
            return view('content.404', [
                'data' => '',
            ]);
        }

        // dd($data);





        return view('content.position', [
            // 'data' => $response,
            'data' => $data,
            // 'data' => $response,
            // 'stack' => $stack,

        ]);
    }
}

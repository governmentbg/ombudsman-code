<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\App;
use Illuminate\Support\Facades\DB;

class MFaqController extends Controller
{
    public static function fqList($locale, $count, $type = 1)
    {

        App::setLocale($locale);
        $lng = i18n($locale);



        $FqList = DB::table('m_faq as n')
            ->join('m_faq_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Fq_id', '=', 'n.Fq_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })



            ->whereNull('n.deleted_at')
            ->whereNull('n18n.deleted_at')
            ->where('n.Fq_type', $type)
            ->select(
                'n.Fq_date',
                'n.Fq_id',
                'n18n.FqL_title',
                'n18n.FqL_path',
                'n18n.FqL_body',
                // 'img.Fq_id'
            )
            ->orderBy('n.Fq_order', 'desc')
            ->orderBy('n.Fq_date', 'desc')
            ->orderBy('n.Fq_id', 'desc')
            ->take($count)
            ->get();
        // ->orderBy('n.Fq_order', 'desc')





        return $FqList;
    }

    public function getFaq($locale, $slug)
    {
        // dd($slug);

        App::setLocale($locale);
        $lng = i18n($locale);
        $id = $slug;

        $data =  DB::table('m_faq as art')
            ->join('m_faq_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Fq_id', '=', 'art.Fq_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })


            ->where('n18n.FqL_path', $id)

            ->whereNull('art.deleted_at')
            ->whereNull('n18n.deleted_at')
            // ->select()

            ->first();

        if (!$data) {
            // pass to 404
            return view('content.404', [
                'data' => '',
            ]);
        }

        // dd($data);


        return view('content.faq', [
            // 'data' => $response,
            'data' => $data,
            // 'data' => $response,
            // 'stack' => $stack,

        ]);
    }
}

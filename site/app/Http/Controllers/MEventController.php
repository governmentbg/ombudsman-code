<?php


namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\App;
use Illuminate\Support\Facades\DB;


class MEventController extends Controller
{
    public function getEvent($locale, $slug)
    {
        // dd($slug);

        App::setLocale($locale);
        $lng = i18n($locale);
        $id = $slug;

        $data =  DB::table('m_event as ev')
            ->join('m_event_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Mv_id', '=', 'ev.Mv_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })
            ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')
            ->leftjoin('m_stream as s', 's.Mv_id', '=', 'ev.Mv_id')


            ->where('n18n.MvL_path', $id)

            ->whereNull('ev.deleted_at')
            ->whereNull('n18n.deleted_at')


            ->first();

        if (!$data) {
            // pass to 404
            return view('content.404', [
                'data' => '',
            ]);
        }

        // dd($data);


        return view('content.event', [
            // 'data' => $response,
            'data' => $data,
            // 'data' => $response,
            // 'stack' => $stack,

        ]);
    }
}

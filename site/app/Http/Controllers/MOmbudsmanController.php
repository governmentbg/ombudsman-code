<?php

namespace App\Http\Controllers;

use Illuminate\Support\Facades\App;
use Illuminate\Support\Facades\DB;
use Illuminate\Http\Request;

class MOmbudsmanController extends Controller
{
    public static function getCurrentOmb($locale)
    {

        App::setLocale($locale);
        $lng = i18n($locale);
        $omb =  DB::table('m_ombudsman as m')
            ->join('m_ombudsman_lng as m18n', function ($join) use ($lng) {
                $join->on('m18n.Omb_id', '=', 'm.Omb_id')
                    ->where('m18n.S_Lng_id', '=',  $lng);
            })
            ->leftjoin('m_article_lng as ar18n', function ($join) use ($lng) {
                $join->on('ar18n.Ar_id', '=', 'm.Ar_id')
                    ->where('ar18n.S_Lng_id', '=',  $lng);
            })


            ->whereNull('m.deleted_at')
            ->whereNull('m18n.deleted_at')
            ->orderBy('m.Omb_date_from', 'desc')
            // ->whereNull('ar18n.deleted_at')

            ->first();

        // dd($omb);

        if ($omb) {
            return $omb;
        } else {
            return null;
        }
    }
}

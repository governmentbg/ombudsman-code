<?php

namespace App\Http\Controllers;

use App\Models\MFeedback;
use App\Models\MFiles;
use App\Models\MGallery;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\App;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Http;

class CommonController extends Controller
{
    public static function getLng($locale)
    {

        App::setLocale($locale);
        $lng = i18n($locale);
        return DB::table('s_lang')
            ->where('S_Lng_id', '!=', $lng)
            ->where('St_id', '=', 1)
            ->whereNull('deleted_at')

            ->get();
    }



    public function search($locale, Request $request)
    {
        $key = $request->key;
        App::setLocale($locale);
        $lng = i18n($locale);
        $article = DB::table("m_article_lng as T")

            ->where('T.S_Lng_id', $lng)
            ->where('T.St_id', 1)
            ->whereNull('T.deleted_at')
            // ->where('T.S_Lng_id', $key)
            ->where(function ($cnt) use ($key) {

                $cnt->orWhere('T.ArL_title', 'like', "%$key%");
                $cnt->orWhere('T.ArL_intro', 'like', "%$key%");
                $cnt->orWhere('T.ArL_body', 'like', "%$key%");
            })
            ->select(
                'T.Ar_id as sID',
                'T.ArL_path as sPath',
                'T.ArL_title as sTitle',
                'T.created_at as sDate',
                DB::raw("'p' as slug"),
                DB::raw("'article' as sHead"),

            );

        $news = DB::table("m_news_lng as T")

            ->where('T.S_Lng_id', $lng)
            ->where('T.St_id', 1)
            ->whereNull('T.deleted_at')
            ->where(function ($cnt) use ($key) {

                $cnt->orWhere('T.MnL_title', 'like', "%$key%");
                $cnt->orWhere('T.MnL_intro', 'like', "%$key%");
                $cnt->orWhere('T.MnL_body', 'like', "%$key%");
            })
            ->select(
                'T.Mn_id as sID',
                'T.MnL_path as sPath',
                'T.MnL_title as sTitle',
                'T.created_at as sDate',
                DB::raw("'n' as slug"),
                DB::raw("'news' as sHead"),

            );

        if ($lng == 1) {

            $position = DB::table("m_position as T")


                ->whereNull('T.deleted_at')
                ->where(function ($cnt) use ($key) {

                    $cnt->orWhere('T.Pst_name', 'like', "%$key%");
                    $cnt->orWhere('T.Pst_body', 'like', "%$key%");
                    $cnt->orWhere('T.Pst_desc', 'like', "%$key%");
                })
                ->select(
                    'T.Pst_id as sID',
                    'T.Pst_path as sPath',
                    'T.Pst_name as sTitle',
                    'T.created_at as sDate',
                    DB::raw("'d' as slug"),
                    DB::raw("'position' as sHead"),

                );
        }

        $faq = DB::table("m_faq_lng as T")

            ->where('T.S_Lng_id', $lng)
            // ->where('T.St_id', 1)
            ->whereNull('T.deleted_at')
            ->where(function ($cnt) use ($key) {

                $cnt->orWhere('T.FqL_title', 'like', "%$key%");
                $cnt->orWhere('T.FqL_body', 'like', "%$key%");
            })
            ->select(
                'T.Fq_id as sID',
                'T.FqL_path as sPath',
                'T.FqL_title as sTitle',
                'T.created_at as sDate',
                DB::raw("'f' as slug"),
                DB::raw("'faq' as sHead"),

            );


        $event = DB::table("m_event_lng as T")

            ->where('T.S_Lng_id', $lng)
            // ->where('T.St_id', 1)
            ->whereNull('T.deleted_at')
            ->where(function ($cnt) use ($key) {

                $cnt->orWhere('T.MvL_title', 'like', "%$key%");
                $cnt->orWhere('T.MvL_body', 'like', "%$key%");
            })
            ->select(
                'T.Mv_id as sID',
                'T.MvL_path as sPath',
                'T.MvL_title as sTitle',
                'T.created_at as sDate',
                DB::raw("'e' as slug"),
                DB::raw("'event' as sHead"),

            )
            ->union($article)
            ->union($news)
            ->union($faq);
        if ($lng == 1) {
            $event->union($position);
        }
        $full = $event->get();
        // dd($full);
        // return $full;

        return view('content.result_list', [
            'res' => $full,
            'key' => $key,
            'count' => count($full),

            // 'stack' => $stack,

        ]);
    }

    public static function getActiveVideo($locale)
    {
        App::setLocale($locale);
        $lng = i18n($locale);

        $data =  DB::table('m_event as ev')
            ->join('m_event_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Mv_id', '=', 'ev.Mv_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })
            ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')
            ->join('m_stream as s', 's.Mv_id', '=', 'ev.Mv_id')



            ->where('n18n.St_id', 1)


            ->whereNull('ev.deleted_at')
            ->whereNull('n18n.deleted_at')
            ->whereNull('s.deleted_at')
            ->whereNotNull('s.Str_home')
            ->where(function ($query) {
                $query->whereNotNull('s.Str_url')
                    ->orWhereNotNull('s.Str_embed');
            })
            // ->whereNull('ev.Mv_date')
            // ->select()
            ->orderBy('Mv_date', 'desc')

            ->exists();

        // https://www.youtube.com/watch?v=X_lFFLpHuSQ



        return $data;
    }

    public static function showActiveVideo($locale)
    {
        App::setLocale($locale);
        $lng = i18n($locale);

        $data =  DB::table('m_event as ev')
            ->join('m_event_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Mv_id', '=', 'ev.Mv_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })
            ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')
            ->join('m_stream as s', 's.Mv_id', '=', 'ev.Mv_id')



            ->where('n18n.St_id', 1)


            ->whereNull('ev.deleted_at')
            ->whereNull('n18n.deleted_at')
            ->whereNotNull('s.Str_home')
            // ->whereNull('ev.Mv_date')
            // ->select()
            ->orderBy('Mv_date', 'desc')

            ->first();



        return $data;
    }



    public function sendForm($locale, Request $request)

    {
        App::setLocale($locale);
        $lng = i18n($locale);
        // $id = $slug;
        // dd($locale);

        $data =  DB::table('m_article as nav')
            ->join('m_article_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Ar_id', '=', 'nav.Ar_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })
            ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')



            ->where('n18n.St_id', 1)
            ->where('n18n.Ar_id', 42)
            // ->where('n18n.ArL_path', $id)

            ->whereNull('nav.deleted_at')
            ->whereNull('n18n.deleted_at')
            // ->select()

            ->first();

        $nav["count"] = NavigationController::navListCurrent($lng, $data->Ar_id, 1);
        $nav["data"] = NavigationController::navListCurrent($lng, $data->Ar_id, 0);
        $data->files = MFiles::fileList('ArL_id', $data->ArL_id);
        // Gallery
        $data->gallery = MGallery::mediaList($lng, 'Ar_id', $data->Ar_id);

        // $request->date = dateToMs($request->date);
        $error = [];

        if (!$request->name && !$request->family) {
            $error["name"] = trans('common.error.name');
        }


        if (!$request->phone && !$request->email) {
            $error["contact"] = trans('common.error.common_details');
        }


        if (!$request->request1) {
            $error["request"] = trans('common.error.request');
        }
        $res = [];
        // $data = [];
        $msg = '';
        if (!$error) {

            // dd($res["city"]);
            // $res = RegisterController::getReg3($lng);

            $msg = trans('common.claim.confirmation');

            $feedback = new MFeedback(array(

                'F_Name' => trim($request["name"]),
                'F_Family' => trim($request["family"]),
                'F_Mail' => trim($request["email"]),
                'F_Phone' => trim($request["phone"]),
                'F_Request' => trim($request["request1"]),



            ));
            $feedback->save();


            $request["name"] = '';
            $request["phone"] = '';
            $request["email"] = '';
            $request["family"] = '';
            $request["request1"] = '';
        }


        // $res = [];
        // $view = 'content.claim_result';
        $subnav = NavigationController::navListParent($lng, $data->Ar_id);
        return view('content.article', [
            'res' => $res,
            'data' => $data,
            'error' => $error,
            'req' => $request,
            'msg' => $msg,
            'nav' => $nav,
            'subnav' => $subnav,


        ]);
    }
}

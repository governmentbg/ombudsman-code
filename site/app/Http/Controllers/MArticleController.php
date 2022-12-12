<?php

namespace App\Http\Controllers;

use App\Models\MArticle;
use App\Models\MArticleLng;
use App\Models\MEvent;
use App\Models\MFiles;
use App\Models\MGallery;
use App\Models\MGalleryLng;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\App;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Http;

class MArticleController extends Controller
{
    public function index()
    {
        // 
        return view('layouts.landing', [
            'data' => '',
        ]);
    }


    public function getArticle($locale, $slug, Request $request)
    {
        // dd($request->key);
        // 
        // dd(1);
        // $response = Http::get('https://test.indexbg.bg/Omb/rest/v1/ombRegisterServices/getRegisterSamosez?pageSize=20');

        // ddd();

        App::setLocale($locale);
        $lng = i18n($locale);
        $id = $slug;
        // dd($locale);

        $data =  DB::table('m_article as nav')
            ->join('m_article_lng as n18n', function ($join) use ($lng) {
                $join->on('n18n.Ar_id', '=', 'nav.Ar_id')
                    ->where('n18n.S_Lng_id', '=',  $lng);
            })
            ->join('s_lang as lng', 'lng.S_Lng_id', '=', 'n18n.S_Lng_id')



            ->where('n18n.St_id', 1)
            ->where('n18n.ArL_path', $id)

            ->whereNull('nav.deleted_at')
            ->whereNull('n18n.deleted_at')
            // ->select()

            ->first();

        if (!$data) {
            return redirect('/');
        }

        $nav["count"] = NavigationController::navListCurrent($lng, $data->Ar_id, 1);
        $nav["data"] = NavigationController::navListCurrent($lng, $data->Ar_id, 0);

        // dd($data);



        if (!$data) {
            // pass to 404
            return view('content.404', [
                'data' => '',
            ]);
        } else {



            // Files

            $data->files = MFiles::fileList('ArL_id', $data->ArL_id);
            // Gallery
            $data->gallery = MGallery::mediaList($lng, 'Ar_id', $data->Ar_id);

            // Sub arrays
            $res = [];
            $error = [];
            $info = [];
            $req = [];

            $msg = '';
            $view = 'content.article';
            if ($data->Ar_id == 42) {

                $req = [
                    'name' => '',
                    'family' => '',
                    'phone' => '',
                    'email' => '',

                    'request1' => '',


                ];
            } elseif ($data->Ar_id == 46) {

                $res = RegisterController::getReg1($lng, $request);

                $res->katNar = RegisterController::getClassif($lng, 201);
                $res->vidNar = RegisterController::getClassif($lng, 202);
                $res->zasPrava = RegisterController::getClassif($lng, 203);
                $res->vidOpl = RegisterController::getClassif($lng, 204);
                $res->sast = RegisterController::getClassif($lng, 209);
                $view = 'content.reg1';
            } elseif ($data->Ar_id == 47) {

                $res = RegisterController::getReg2($lng, $request);

                $res->narPrava = RegisterController::getClassif($lng, 211);
                $res->codeOrgan = RegisterController::getClassif($lng, 216);
                $res->prepor = RegisterController::getClassif($lng, 26);
                $res->vidResult = RegisterController::getClassif($lng, 217);
                $res->sast = RegisterController::getClassif($lng, 219);

                $view = 'content.reg2';
            } elseif ($data->Ar_id == 48) {

                $res = RegisterController::getReg3($lng, $request);

                $res->zasPrava = RegisterController::getClassif($lng, 203);
                $res->codeOrgan = RegisterController::getClassif($lng, 8);
                $res->prepor = RegisterController::getClassif($lng, 26);
                $res->vidResult = RegisterController::getClassif($lng, 217);
                $res->sast = RegisterController::getClassif($lng, 219);

                $view = 'content.reg3';
            } elseif ($data->Ar_id == 15) {

                $res = MPositionController::pstList(10, 1);
                App::setLocale($locale);
                $view = 'content.pst_list';
                // dd($locale);
            } elseif ($data->Ar_id == 51) {

                $res = MPositionController::pstList(10, 2);
                App::setLocale($locale);
                $view = 'content.pst_list';
                // dd($locale);
            } elseif ($data->Ar_id == 40) {
                // dd($lng);
                $res = MNewsController::frontNews($locale, 10, 0);
                App::setLocale($locale);

                $view = 'content.news_list';
                // dd($locale);
            } elseif ($data->Ar_id == 52) {
                // dd($lng);
                $res = MNewsController::frontNews($locale, 10, 0, 2, 0);
                App::setLocale($locale);
                $view = 'content.actual_list';
                // dd($locale);
            } elseif ($data->Ar_id == 41) {

                $res = MEvent::getEvents($lng);
                $view = 'content.event_list';
            }
            // elseif ($data->Ar_id == 44) {


            //                 $view = 'content.faq_box';
            //             } 
            elseif ($data->Ar_id == 49) {
                // dd();
                // dd(env('name'));
                // $res["test"] = RegisterController::getClassif($lng, 2);
                $res["citizen"] = RegisterController::getClassif($lng, 22);
                $res["rights"] = RegisterController::getClassif($lng, 203);
                // $res["vid"] = RegisterController::getClassif($lng, 18);
                $res["answer"] = RegisterController::getClassif($lng, 222);
                // dd($res["answer"]);
                $res["city"] = RegisterController::getCity($lng);
                $res["gender"] = RegisterController::getGender();
                $res["vid"] = [];
                // dd($res["city"]);
                // $res = RegisterController::getReg3($lng);

                $req = [
                    'persType' => 4,
                    'name' => '',
                    'name1' => '',
                    'fileBox' => '',
                    'claimKey' => claimKey(),
                    'egn' => '',
                    'lnch' => '',
                    'eik' => '',
                    'age' => '',
                    'zip' => '',
                    'address' => '',
                    'phone' => '',
                    'email' => '',
                    'defendant' => '',
                    'date' => '',
                    'rights' => '',
                    'city' => '',
                    'citizen' => '',
                    'protect' => '',
                    'gender' => '',
                    'descr' => '',
                    'request1' => '',
                    'consideredBy' => '',
                    'considered' => 0,
                    'answer' => '',
                    'vid' => '',
                    'fileList' => [],

                ];
                // $req["name"] = '';
                $view = 'content.claim';
            } elseif ($data->Ar_id == 39) {

                $res["city"] = RegisterController::getCity($lng);
                $res["answer"] = RegisterController::getClassif($lng, 222);

                $req = [
                    'name' => '',
                    'claimKey' => claimKey(),
                    'name1' => '',
                    'egn' => '',
                    'lnch' => '',
                    'eik' => '',
                    'age' => '',
                    'zip' => '',
                    'city' => '',
                    'address' => '',
                    'phone' => '',
                    'email' => '',
                    'defendant' => '',
                    'date' => '',
                    'descr' => '',
                    'request1' => '',
                    'consideredBy' => '',
                    'answer' => '',

                ];
                // $res["city"] = RegisterController::getCity($lng);
                // dd($res["city"]);
                // $res = RegisterController::getReg3($lng);
                $view = 'content.claim_child';
            } else {
                $view = 'content.article';
            }

            $subnav = NavigationController::navListParent($lng, $data->Ar_id);


            return view($view, [
                'res' => $res,
                'data' => $data,
                'nav' => $nav,
                'error' => $error,
                'info' => $info,
                'subnav' => $subnav,
                'req' => $req,
                'msg' => $msg,

            ]);
        }
    }
}

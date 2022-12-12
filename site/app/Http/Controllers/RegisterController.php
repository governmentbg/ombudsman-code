<?php

namespace App\Http\Controllers;


use Illuminate\Http\Request;
// use Illuminate\Http\Response;
use Illuminate\Support\Facades\Response;
use Illuminate\Support\Facades\Http;

class RegisterController extends Controller
{
    //
    public static function getClassif($locale, $id)
    {
        $response =  Http::get(config('app.backend_url') . "v1/ombClassifServices/getClassif/{$id}", [

            'lang' => $locale,
            'sort ' => 'asc',

        ]);



        return $response->json();
    }



    public static function getGender()
    {
        $response =   [
            0 => [

                'code' => 1,
                'tekst' => trans('common.claim.male')
            ],
            1 => [

                'code' => 2,
                'tekst' => trans('common.claim.female')
            ]
        ];



        return $response;
    }

    public static function codeByClassif($locale, $id)
    {

        $response =  Http::get(config('app.backend_url') . "v1/ombClassifServices/getLogicListByCode/22/{$id}", [

            'lang' => $locale,

        ]);



        return $response->json();
    }

    public static function getCity($locale)
    {
        // dd(10);

        $response =  Http::get(config('app.backend_url') . "v1/ombClassifServices/getNasMiasto?query=&lang=1&sort=asc", [

            'lang' => $locale,

        ]);


        return $response->json();
    }

    public static function getReg1($locale, $request)
    {

        // $egn =  Http::withHeaders(['Content-Type' => 'application/json'])
        //     ->post(config('app.backend_url') . "ombValidationServices/validateEgn", [

        //         'egn' => '9912080070',

        //     ]);

        // dd($egn->json());
        // $eik_test = '1304049712';

        // $eik =  Http::withHeaders(['Content-Type' => 'application/json'])
        //     ->post(config('app.backend_url') . "ombValidationServices/validateEik", [

        //         'eik' => $eik_test,

        //     ]);

        // dd($eik->json());

        // Http::

        $pageIndex =  ($request->pageIndex ? $request->pageIndex : null);
        $docDateOt =  ($request->docDateOt ? dateToMs($request->docDateOt) : null);
        $docDateDo =  ($request->docDateDo ? dateToMs($request->docDateDo) : null);
        $rnDoc  =  ($request->rnDoc  ? $request->rnDoc  : null);
        $jbpType =  ($request->jbpType ? $request->jbpType : null);
        $katNar =  ($request->katNar ? $request->katNar : null);
        $vidNar =  ($request->vidNar ? $request->vidNar : null);
        $zasPrava =  ($request->zasPrava ? $request->zasPrava : null);
        $vidOpl =  ($request->vidOpl ? $request->vidOpl : null);
        $sast =  ($request->sast ? $request->sast : null);


        $req =  Http::get(config('app.backend_url') . "v1/ombRegisterServices/getRegisterJalbi", [
            'pageSize' => 150,
            'pageIndex' => $pageIndex,
            'lang' => $locale,
            'rnDoc' =>  $rnDoc,
            'docDateOt' =>  $docDateOt,
            'docDateDo' => $docDateDo,
            'jbpType' => $jbpType,
            'katNar' =>  $katNar,
            'vidNar' => $vidNar,
            'zasPrava' => $zasPrava,
            'vidOpl' => $vidOpl,
            'sast' => $sast,
        ]);

        // dd($req);



        return $req;
    }


    public static function getReg2($locale, $request)
    {

        $pageIndex =  ($request->pageIndex ? $request->pageIndex : null);
        $docDateOt =  ($request->docDateOt ? dateToMs($request->docDateOt) : null);
        $docDateDo =  ($request->docDateDo ? dateToMs($request->docDateDo) : null);
        $rnDoc =  ($request->rnDoc ? $request->rnDoc : null);
        $predmet =  ($request->predmet ? $request->predmet : null);
        $narPrava =  ($request->narPrava ? $request->narPrava : null);
        $codeOrgan =  ($request->codeOrgan ? $request->codeOrgan : null);
        $konstat =  ($request->konstat ? $request->konstat : null);
        $prepor =  ($request->prepor ? $request->prepor : null);
        $vidResult =  ($request->vidResult ? $request->vidResult : null);
        $sast =  ($request->sast ? $request->sast : null);
        $req =  Http::get(config('app.backend_url') . "v1/ombRegisterServices/getRegisterNpm", [
            'pageSize' => '150',
            'pageIndex' => $pageIndex,
            'lang' => $locale,
            'docDateOt' => $docDateOt,
            'docDateDo' => $docDateDo,
            'rnDoc' => $rnDoc,
            'predmet' => $predmet,
            'narPrava' => $narPrava,
            'codeOrgan' => $codeOrgan,
            'konstat' => $konstat,
            'prepor' => $prepor,
            'vidResult' => $vidResult,
            'sast' => $sast,
        ]);
        // dd($req);

        return $req;
    }


    public static function getReg3($locale, $request)
    {

        $pageIndex =  ($request->pageIndex ? $request->pageIndex : null);
        $docDateOt =  ($request->docDateOt ? dateToMs($request->docDateOt) : null);
        $docDateDo =  ($request->docDateDo ? dateToMs($request->docDateDo) : null);
        $rnDoc =  ($request->rnDoc ? $request->rnDoc : null);
        $predmet =  ($request->predmet ? $request->predmet : null);
        $zasPrava =  ($request->zasPrava ? $request->zasPrava : null);
        $codeOrgan =  ($request->codeOrgan ? $request->codeOrgan : null);
        $konstat =  ($request->konstat ? $request->konstat : null);
        $prepor =  ($request->prepor ? $request->prepor : null);
        $vidResult =  ($request->vidResult ? $request->vidResult : null);
        $sast =  ($request->sast ? $request->sast : null);

        // dd($zasPrava);

        return Http::get(config('app.backend_url') . "v1/ombRegisterServices/getRegisterSamosez", [
            'pageSize' => 150,
            'pageIndex' => $pageIndex,
            'lang' => $locale,
            'rnDoc' => $rnDoc,
            'docDateOt' => $docDateOt,
            'docDateDo' => $docDateDo,
            'zasPrava' => $zasPrava,
            'codeOrgan' => $codeOrgan,
            'predmet' => $predmet,
            'konstat' => $konstat,
            'prepor' => $prepor,
            'vidResult' => $vidResult,
            'sast' => $sast,
        ]);
    }
}

<?php

namespace App\Nova;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use KossShtukert\LaravelNovaSelect2\Select2;
use Laravel\Nova\Fields\BelongsTo;
use Laravel\Nova\Fields\Boolean;
use Laravel\Nova\Fields\Date;
use Laravel\Nova\Fields\HasMany;
use Laravel\Nova\Fields\Hidden;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Fields\Select;
use Laravel\Nova\Fields\Text;
use Illuminate\Support\Str;
use Laravel\Nova\Http\Requests\NovaRequest;

class MArticle extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\MArticle::class;
    // public static $globallySearchable = false;

    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'Ar_name';
    public static function label()
    {
        return 'Секции';
    }

    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'Ar_name',
    ];



    public static function userLevel(Request $request)
    {

        return DB::table('users')
            ->where('id', $request->user()->id)
            ->select('Ul_id')
            ->first()->Ul_id;
    }

    public static function assignedАr(Request $request)
    {
        return DB::table('u_level2article')
            ->where('Ul_id', self::userLevel($request))
            ->select('Ar_id')
            ->get()->pluck('Ar_id');
    }

    public static function indexQuery(NovaRequest $request, $query)
    {
        if (self::userLevel($request) > 1) {

            return $query->whereIn('Ar_id', self::assignedАr($request))
                ->orWhereIn('Ar_parent_id', self::assignedАr($request));
        }
    }

    /**
     * Get the fields displayed by the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function fields(Request $request)
    {

        $nav = DB::table('m_article')
            ->select('Ar_name as name', 'Ar_id as id');
        if (self::userLevel($request) > 1) {

            $nav->whereIn('Ar_id', self::assignedАr($request))
                ->orWhereIn('Ar_parent_id', self::assignedАr($request));
        }

        $nav =  $nav->get()->pluck('name', 'id');


        return [
            ID::make(__('ID'), 'Ar_id')->sortable(),
            // Text::make('Име', 'Ar_name')->sortable()->rules('required'),
            Text::make('Заглавие', 'Ar_name')->sortable()->rules('required')->displayUsing(function ($text) {
                return Str::limit($text, 60);
            }),
            //  Hidden::make('Ar_parent_id', 'Ar_parent_id')->default(function ($request) {
            //     return $request->viaResourceId;
            // }),
            Select2::make('Parent', 'Ar_parent_id')
                ->sortable()
                ->required()
                ->options($nav)
                ->displayUsingLabels()
                ->showAsLink()
                ->onlyOnForms()
                ->configuration([
                    'placeholder'             => __('Choose an option'),
                    'allowClear'              => true,
                    'minimumResultsForSearch' => 1,
                    'multiple'                => false,
                ]),
            Date::make('Дата', 'Ar_date')->sortable(),
            Text::make('Подредба', 'Ar_order')->sortable(),
            // Text::make('Подредба', 'Ar_order')->creationRules('integer')->updateRules('integer')->sortable(),
            HasMany::make('Езиковa версия', 'eq_lng', MArticleLng::class),
            Select::make('Вид', 'Ar_type')->options([
                1 => 'Основна',
                // 2 => 'Допълнителна',
                3 => 'Детски подсайт'
            ])->displayUsingLabels()->required(),
            Boolean::make('Активна навигация', 'Ar_menu'),
            HasMany::make('Снимки', 'eq_gallery', MGallery::class),
            BelongsTo::make('Parent', 'parent', MArticle::class)->sortable()->nullable()->withoutTrashed()->hideWhenCreating()->hideWhenUpdating()->required(),
            HasMany::make('Child', 'children', MArticle::class),
        ];
    }

    /**
     * Get the cards available for the request.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function cards(Request $request)
    {
        return [];
    }

    /**
     * Get the filters available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function filters(Request $request)
    {
        return [];
    }

    /**
     * Get the lenses available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function lenses(Request $request)
    {
        return [];
    }

    /**
     * Get the actions available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function actions(Request $request)
    {
        return [];
    }
}

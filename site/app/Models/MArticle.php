<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Str;

/**
 * @property int    $Ar_id
 * @property int    $Ar_parent_id
 * @property int    $created_at
 * @property int    $updated_at
 * @property int    $deleted_at
 * @property string $Ar_name
 * @property Date   $Ar_date
 */
class MArticle extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_article';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Ar_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Ar_parent_id', 'Ar_type', 'Ar_menu', 'Cat_id', 'Ar_name', 'Ar_date', 'Ar_order', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'Ar_id' => 'int', 'Ar_parent_id' => 'int', 'Ar_type' => 'int', 'Ar_menu' => 'int', 'Cat_id' => 'int', 'Ar_name' => 'string', 'Ar_date' => 'date',  'Ar_order' => 'int', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'Ar_date', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();

        static::creating(function ($article) {
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {
            $article->updated_at = now();
        });


        self::created(function ($article) {
            $truncated = substr($article->Ar_name, 0, 80) . "-" . $article->Ar_id;

            MArticleLng::create([
                'Ar_id' => $article->Ar_id,
                'S_Lng_id' => 1,
                'ArL_path' =>  Str::slug(C2L($truncated)),
                'ArL_title' => $article->Ar_name,
            ]);
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...


    public function eq_lng()
    {
        return $this->hasMany(MArticleLng::class, 'Ar_id');
    }

    public function eq_gallery()
    {
        return $this->hasMany(MGallery::class, 'Ar_id');
    }

    public function parent()
    {
        return $this->belongsTo(MArticle::class, 'Ar_parent_id', 'Ar_id');
    }

    public function children()
    {
        return $this->hasMany(MArticle::class, 'Ar_parent_id', 'Ar_id');
    }

    public function ul_articles()
    {
        return $this->hasMany(ULevel2article::class,  'Ar_id');
    }
}
